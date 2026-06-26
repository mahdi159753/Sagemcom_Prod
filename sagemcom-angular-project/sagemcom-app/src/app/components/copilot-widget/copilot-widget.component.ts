import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CopilotService } from '../../services/copilot.service';
import { marked } from 'marked';

interface ChatBubble {
  sender: 'user' | 'ai';
  content: string;
  isHtml?: boolean;
}

@Component({
  selector: 'app-copilot-widget',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './copilot-widget.component.html',
  styleUrls: ['./copilot-widget.component.scss']
})
export class CopilotWidgetComponent implements OnInit {
  isOpen: boolean = false;
  messages: ChatBubble[] = [];
  messageText: string = '';
  isLoading: boolean = false;

  @ViewChild('scrollMe') private myScrollContainer!: ElementRef;

  constructor(private copilotService: CopilotService) {}

  ngOnInit(): void {
    // Initial greeting
    this.messages.push({
      sender: 'ai',
      content: "👋 Bonjour ! Je suis l'Agent Copilot IA de Sagemcom. Je peux analyser les performances, les arrêts ou les non-conformités. Comment puis-je vous aider ?",
      isHtml: false
    });
  }

  toggleWidget(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.scrollToBottom();
    }
  }

  async sendMessage(): Promise<void> {
    if (!this.messageText.trim()) return;

    const userMessage = this.messageText;
    this.messageText = '';
    
    // Add user message to UI
    this.messages.push({ sender: 'user', content: userMessage });
    this.scrollToBottom();

    this.isLoading = true;

    this.copilotService.sendMessage(userMessage).subscribe({
      next: async (res) => {
        this.isLoading = false;
        // Parse markdown from response
        const htmlContent = await marked.parse(res.reply);
        this.messages.push({ sender: 'ai', content: htmlContent, isHtml: true });
        this.scrollToBottom();
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
        this.messages.push({ sender: 'ai', content: "⚠️ Désolé, je n'ai pas pu joindre le serveur. Vérifiez votre connexion.", isHtml: false });
        this.scrollToBottom();
      }
    });
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
}
