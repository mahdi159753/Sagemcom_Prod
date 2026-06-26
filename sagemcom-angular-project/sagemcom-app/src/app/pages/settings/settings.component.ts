import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {
  // Config state
  siteName       = 'Sagemcom Tunisia';
  defaultChantier = 'INTEG';
  workingHours   = '08:00 – 17:00';
  
  // Security state
  timeoutMinutes = 30;
  twoFactor      = false;
  sessionTimeout = true;

  // Notifications state
  downtimeAlerts = true;
  trgAlerts      = true;
  dailyReport    = false;

  // Backup state
  backupTime     = '02:00';
  retention      = '365';
  autoBackup     = true;

  // Toast state
  toast = { visible: false, message: '', type: 'success' };
  private toastTimeout: any;

  ngOnInit() {
    this.loadSettings();
  }

  loadSettings() {
    const saved = localStorage.getItem('sagemcom_settings');
    if (saved) {
      try {
        const data = JSON.parse(saved);
        Object.assign(this, data);
      } catch (e) {
        console.error('Failed to parse settings', e);
      }
    }
  }

  onSaveAll() {
    const dataToSave = {
      siteName: this.siteName,
      defaultChantier: this.defaultChantier,
      workingHours: this.workingHours,
      timeoutMinutes: this.timeoutMinutes,
      twoFactor: this.twoFactor,
      sessionTimeout: this.sessionTimeout,
      downtimeAlerts: this.downtimeAlerts,
      trgAlerts: this.trgAlerts,
      dailyReport: this.dailyReport,
      backupTime: this.backupTime,
      retention: this.retention,
      autoBackup: this.autoBackup
    };
    
    localStorage.setItem('sagemcom_settings', JSON.stringify(dataToSave));
    this.showToast('All settings saved successfully!', 'success');
  }

  onSaveConfig() {
    this.onSaveAll();
  }

  onChangePassword() { 
    this.showToast('Password change dialog triggered.', 'info'); 
  }

  onExportData() { 
    this.showToast('Data export started...', 'info'); 
  }

  onReset() { 
    localStorage.removeItem('sagemcom_settings');
    this.showToast('Settings reset. Please refresh the page.', 'error'); 
  }

  showToast(message: string, type: 'success' | 'error' | 'info' = 'success') {
    this.toast = { visible: true, message, type };
    if (this.toastTimeout) clearTimeout(this.toastTimeout);
    this.toastTimeout = setTimeout(() => {
      this.toast.visible = false;
    }, 3000);
  }
}
