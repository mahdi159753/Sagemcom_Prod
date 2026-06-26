import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './auth.service';

export interface ChatMessage {
  id?: number;
  senderId: number;
  recipientId: number;
  content: string;
  attachmentUrl?: string;
  timestamp?: string;
  status?: string;
}

export interface UserContact {
  id: number;
  firstname: string;
  lastname: string;
  email: string;
  lastActive: string;
  unreadCount?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private baseUrl = '/api/v1';
  private stompClient: Client | null = null;
  
  private messagesSubject = new BehaviorSubject<ChatMessage[]>([]);
  public messages$ = this.messagesSubject.asObservable();
  
  private activeContactSubject = new BehaviorSubject<UserContact | null>(null);
  public activeContact$ = this.activeContactSubject.asObservable();

  constructor(private http: HttpClient, private authService: AuthService, private ngZone: NgZone) {
    if (this.authService.isLoggedIn()) {
      this.connectWebSocket();
    }
  }

  private connectWebSocket() {
    const currentUserId = this.authService.getUserId();
    if (!currentUserId) return;

    this.stompClient = new Client({
      webSocketFactory: () => new (SockJS as any)('/ws-notifications'),
      reconnectDelay: 5000,
      debug: (str) => { }
    });

    this.stompClient.onConnect = (frame) => {
      // Subscribe to user's specific topic
      this.stompClient?.subscribe(`/topic/messages/${currentUserId}`, (message: IMessage) => {
        this.ngZone.run(() => {
          if (message.body) {
            const newMsg: ChatMessage = JSON.parse(message.body);
            const activeContact = this.activeContactSubject.value;
            
            // If message belongs to active conversation, append it
            if (activeContact && (newMsg.senderId === activeContact.id || newMsg.recipientId === activeContact.id)) {
              const currentMsgs = this.messagesSubject.value;
              this.messagesSubject.next([...currentMsgs, newMsg]);
            } else {
              // Otherwise it's a message from someone else, we might want to update unread counts here
            }
          }
        });
      });
    };

    this.stompClient.activate();
  }

  public getContacts(): Observable<UserContact[]> {
    return this.http.get<UserContact[]>(`${this.baseUrl}/chat/users`);
  }

  public fetchMessages(recipientId: number): void {
    const currentUserId = this.authService.getUserId();
    if (!currentUserId) return;
    
    this.http.get<ChatMessage[]>(`${this.baseUrl}/messages/${currentUserId}/${recipientId}`)
      .subscribe({
        next: (msgs) => this.messagesSubject.next(msgs),
        error: (err) => console.error('Error fetching messages', err)
      });
  }

  public setActiveContact(contact: UserContact | null) {
    this.activeContactSubject.next(contact);
    if (contact) {
      this.fetchMessages(contact.id);
    } else {
      this.messagesSubject.next([]);
    }
  }

  public sendMessage(content: string, attachmentUrl?: string) {
    const currentUserId = this.authService.getUserId();
    const activeContact = this.activeContactSubject.value;
    
    if (!currentUserId || !activeContact || !this.stompClient || !this.stompClient.active) return;

    const chatMessage: ChatMessage = {
      senderId: currentUserId,
      recipientId: activeContact.id,
      content: content,
      attachmentUrl: attachmentUrl
    };

    this.stompClient.publish({
      destination: '/app/chat',
      body: JSON.stringify(chatMessage)
    });

    // Optimistically add to UI
    const tempMsg: ChatMessage = {
      ...chatMessage,
      timestamp: new Date().toISOString(),
      status: 'SENT'
    };
    const currentMsgs = this.messagesSubject.value;
    this.messagesSubject.next([...currentMsgs, tempMsg]);
  }

  public uploadFile(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);
    // Angular HttpClient will automatically set the correct Content-Type with boundary for FormData
    return this.http.post(`${this.baseUrl}/chat/upload`, formData, { responseType: 'text' });
  }
}
