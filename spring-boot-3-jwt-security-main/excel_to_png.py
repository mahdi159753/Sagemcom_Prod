import os
import sys
import re
import win32com.client
import fitz  # PyMuPDF
import tempfile

def main():
    if len(sys.argv) < 4:
        print("Usage: python excel_to_png.py <excel_path> <output_dir> <prefix>")
        sys.exit(1)
        
    excel_path = os.path.abspath(sys.argv[1])
    output_dir = os.path.abspath(sys.argv[2])
    prefix = sys.argv[3]
    
    print(f"Excel File: {excel_path}")
    print(f"Output Dir: {output_dir}")
    print(f"Prefix: {prefix}")
    
    if not os.path.exists(excel_path):
        print(f"Error: Excel file does not exist at {excel_path}")
        sys.exit(1)
        
    if not os.path.exists(output_dir):
        os.makedirs(output_dir, exist_ok=True)
        
    # Excel automation
    excel = None
    wb = None
    try:
        excel = win32com.client.Dispatch("Excel.Application")
        excel.Visible = False
        excel.DisplayAlerts = False
        
        wb = excel.Workbooks.Open(excel_path)
        
        # Use exact set for skipped sheets (case-insensitive)
        skip_sheets = {"table 1", "annexe", "feuil1"}
        
        for sheet in wb.Sheets:
            sheet_name = sheet.Name
            sheet_name_clean = sheet_name.strip().lower()
            
            # Skip if match skip word exactly
            if sheet_name_clean in skip_sheets:
                print(f"Skipping sheet: {sheet_name} (in skip list)")
                continue
                
            # Extract number from sheet name
            match = re.search(r'\d+', sheet_name)
            if not match:
                print(f"Skipping sheet: {sheet_name} (no number found in name)")
                continue
                
            sheet_num = int(match.group(0))
            
            # Create a temporary PDF file for this sheet
            with tempfile.TemporaryDirectory() as temp_dir:
                pdf_path = os.path.join(temp_dir, "temp_sheet.pdf")
                
                try:
                    # xlTypePDF = 0
                    sheet.ExportAsFixedFormat(0, pdf_path)
                    
                    # Convert PDF to PNG
                    if os.path.exists(pdf_path):
                        doc = fitz.open(pdf_path)
                        if len(doc) > 0:
                            page = doc.load_page(0)
                            # Render with 150 DPI for good resolution
                            pix = page.get_pixmap(dpi=150)
                            
                            png_filename = f"{prefix}_page_{sheet_num:02d}.png"
                            png_path = os.path.join(output_dir, png_filename)
                            
                            pix.save(png_path)
                            print(f"Successfully rendered: {sheet_name} -> {png_filename}")
                        else:
                            print(f"Error: Empty PDF generated for sheet {sheet_name}")
                        doc.close()
                    else:
                        print(f"Error: PDF was not generated for sheet {sheet_name}")
                except Exception as sheet_err:
                    print(f"Error rendering sheet {sheet_name}: {sheet_err}")
                    
    except Exception as e:
        print(f"An error occurred during rendering: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
    finally:
        if wb:
            try:
                wb.Close(False)
            except:
                pass
        if excel:
            try:
                excel.Quit()
            except:
                pass

if __name__ == "__main__":
    main()
