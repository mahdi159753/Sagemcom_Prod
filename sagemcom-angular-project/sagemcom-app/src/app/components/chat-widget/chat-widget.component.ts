import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService, UserContact, ChatMessage } from '../../services/chat.service';
import { AuthService } from '../../services/auth.service';
import { Subscription } from 'rxjs';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-chat-widget',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-widget.component.html',
  styleUrls: ['./chat-widget.component.scss']
})
export class ChatWidgetComponent implements OnInit, OnDestroy {
  isOpen: boolean = false;
  isChatting: boolean = false;
  
  contacts: UserContact[] = [];
  activeContact: UserContact | null = null;
  messages: ChatMessage[] = [];
  currentUserId: number = 0;
  messageText: string = '';
  selectedFile: File | null = null;
  isUploading: boolean = false;
  isChatRoute: boolean = false;
  
  private subs: Subscription = new Subscription();
  @ViewChild('scrollMe') private myScrollContainer!: ElementRef;

  constructor(
    private chatService: ChatService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Check initial route
    this.isChatRoute = this.router.url.includes('/chat');

    // Listen to route changes
    this.subs.add(
      this.router.events.pipe(
        filter(event => event instanceof NavigationEnd)
      ).subscribe((event: any) => {
        this.isChatRoute = event.urlAfterRedirects.includes('/chat');
        if (this.isChatRoute) {
          this.isOpen = false;
        }
      })
    );

    const userId = this.authService.getUserId();
    if (userId) {
      this.currentUserId = userId;
    }

    this.subs.add(
      this.chatService.getContacts().subscribe(res => {
        this.contacts = res;
      })
    );

    this.subs.add(
      this.chatService.activeContact$.subscribe(c => {
        this.activeContact = c;
        if (c && !this.isChatRoute) {
          this.isChatting = true;
          this.isOpen = true;
        }
      })
    );

    this.subs.add(
      this.chatService.messages$.subscribe(msgs => {
        this.messages = msgs;
        this.scrollToBottom();
      })
    );
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  toggleWidget(): void {
    this.isOpen = !this.isOpen;
    if (!this.isOpen) {
      this.isChatting = false;
      this.chatService.setActiveContact(null);
    }
  }

  openChat(contact: UserContact): void {
    this.chatService.setActiveContact(contact);
  }

  backToList(): void {
    this.isChatting = false;
    this.chatService.setActiveContact(null);
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
  
  openFullChat(): void {
     this.isOpen = false;
     this.router.navigate(['/chat']);
  }

  isOnline(lastActive: string): boolean {
    if (!lastActive) return false;
    const last = new Date(lastActive).getTime();
    const now = new Date().getTime();
    return (now - last) < 300000;
  }

  isImage(url: string | undefined): boolean {
    if (!url) return false;
    return url.match(/\.(jpeg|jpg|gif|png|webp)$/i) != null;
  }
}
