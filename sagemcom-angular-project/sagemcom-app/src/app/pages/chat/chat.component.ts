import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService, UserContact, ChatMessage } from '../../services/chat.service';
import { AuthService } from '../../services/auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.scss']
})
export class ChatComponent implements OnInit, OnDestroy {
  contacts: UserContact[] = [];
  filteredContacts: UserContact[] = [];
  activeContact: UserContact | null = null;
  messages: ChatMessage[] = [];
  currentUserId: number = 0;
  messageText: string = '';
  searchQuery: string = '';
  selectedFile: File | null = null;
  isUploading: boolean = false;

  private subs: Subscription = new Subscription();
  @ViewChild('scrollMe') private myScrollContainer!: ElementRef;

  constructor(
    private chatService: ChatService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const userId = this.authService.getUserId();
    if (userId) {
      this.currentUserId = userId;
    }

    // Load contacts
    this.subs.add(
      this.chatService.getContacts().subscribe(res => {
        this.contacts = res;
        this.filteredContacts = res;
      })
    );

    // Track active contact
    this.subs.add(
      this.chatService.activeContact$.subscribe(c => {
        this.activeContact = c;
      })
    );

    // Track messages
    this.subs.add(
      this.chatService.messages$.subscribe(msgs => {
        this.messages = msgs;
        this.scrollToBottom();
      })
    );
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
    this.chatService.setActiveContact(null);
  }

  filterContacts(): void {
    if (!this.searchQuery) {
      this.filteredContacts = this.contacts;
    } else {
      this.filteredContacts = this.contacts.filter(c => 
        c.firstname.toLowerCase().includes(this.searchQuery.toLowerCase()) || 
        c.lastname.toLowerCase().includes(this.searchQuery.toLowerCase())
      );
    }
  }

  selectContact(contact: UserContact): void {
    this.chatService.setActiveContact(contact);
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
    }
  }

  removeAttachment(): void {
    this.selectedFile = null;
  }

  sendMessage(): void {
    if (!this.messageText.trim() && !this.selectedFile) return;
    
    if (this.selectedFile) {
      this.isUploading = true;
      this.chatService.uploadFile(this.selectedFile).subscribe({
        next: (url) => {
          this.chatService.sendMessage(this.messageText, url);
          this.messageText = '';
          this.selectedFile = null;
          this.isUploading = false;
        },
        error: (err) => {
          console.error("Upload failed", err);
          this.isUploading = false;
        }
      });
    } else {
      this.chatService.sendMessage(this.messageText);
      this.messageText = '';
    }
  }

  scrollToBottom(): void {
    setTimeout(() => {
      try {
        if (this.myScrollContainer) {
          this.myScrollContainer.nativeElement.scrollTop = this.myScrollContainer.nativeElement.scrollHeight;
        }
      } catch(err) { }
    }, 100);
  }

  isOnline(lastActive: string): boolean {
    if (!lastActive) return false;
    const last = new Date(lastActive).getTime();
    const now = new Date().getTime();
    // Consider online if active in the last 5 minutes (300000ms)
    return (now - last) < 300000;
  }

  isImage(url: string | undefined): boolean {
    if (!url) return false;
    return url.match(/\.(jpeg|jpg|gif|png|webp)$/i) != null;
  }
}
