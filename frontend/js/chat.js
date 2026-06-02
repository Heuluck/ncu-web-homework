/**
 * 聊天核心逻辑
 */
const ChatManager = {
  currentFriendId: null,
  currentFriendInfo: null,
  currentPage: 1,
  pageSize: 20,
  hasMore: true,
  loading: false,

  async openChat(friendId) {
    if (this.currentFriendId === friendId) return;

    this.currentFriendId = friendId;
    this.currentPage = 1;
    this.hasMore = true;

    const conv = ConversationManager.conversations.find(c => c.friendId === friendId);
    this.currentFriendInfo = conv || { friendId, nickname: '加载中...', avatar: null, onlineStatus: 0 };

    this._renderHeader();
    document.getElementById('chatInputArea').style.display = '';

    const messagesEl = document.getElementById('chatMessages');
    messagesEl.innerHTML = '<div class="chat-loading"><div class="spinner"></div></div>';

    await this.loadHistory(friendId, 1, true);
    this._markAsRead(friendId);
    ConversationManager.clearUnread(friendId);
  },

  _renderHeader() {
    const info = this.currentFriendInfo;
    if (!info) return;

    document.getElementById('chatHeader').style.display = '';
    document.getElementById('chatAvatar').src = Utils.getAvatarUrl(info.avatar, `user-${info.friendId}`);
    document.getElementById('chatAvatar').alt = info.nickname;
    document.getElementById('chatName').textContent = info.nickname;

    const statusEl = document.getElementById('chatStatus');
    statusEl.className = `status-indicator ${Utils.getStatusClass(info.onlineStatus)}`;
    document.getElementById('chatStatusText').textContent = Utils.getStatusText(info.onlineStatus);
  },

  updateHeaderStatus(status) {
    if (!this.currentFriendInfo) return;
    this.currentFriendInfo.onlineStatus = status;
    const statusEl = document.getElementById('chatStatus');
    statusEl.className = `status-indicator ${Utils.getStatusClass(status)}`;
    document.getElementById('chatStatusText').textContent = Utils.getStatusText(status);
  },

  async loadHistory(friendId, pageNum, replace = false) {
    if (this.loading) return;
    this.loading = true;

    const res = await API.get(
      `/api/message/private/history?friendId=${friendId}&pageNum=${pageNum}&pageSize=${this.pageSize}`
    );

    this.loading = false;

    if (res && res.code === 200) {
      const page = res.data;
      const messages = page.records || [];

      if (messages.length < this.pageSize) this.hasMore = false;
      this.currentPage = pageNum;

      if (replace) {
        this._renderMessages(messages, true);
      } else {
        this._prependMessages(messages);
      }
    }
  },

  _renderMessages(messages, scrollBottom = false) {
    const messagesEl = document.getElementById('chatMessages');

    if (messages.length === 0 && this.currentPage === 1) {
      messagesEl.innerHTML = `
        <div class="chat-empty">
          <div class="empty-state">
            <div class="empty-state-icon">
              <i data-lucide="message-square" style="width: 48px; height: 48px;"></i>
            </div>
            <div class="empty-state-title" style="font-size: 14px;">开始你们的对话吧</div>
          </div>
        </div>`;
      lucide.createIcons();
      return;
    }

    let html = '';
    if (this.hasMore) {
      html += '<div class="load-more-wrapper"><button class="btn btn-ghost btn-sm" id="loadMoreBtn">加载更多</button></div>';
    }

    let lastDate = '';
    messages.forEach(msg => {
      const msgDate = this._getDateLabel(msg.createTime);
      if (msgDate !== lastDate) {
        html += `<div class="date-divider"><span>${msgDate}</span></div>`;
        lastDate = msgDate;
      }
      html += this._renderBubble(msg);
    });

    messagesEl.innerHTML = html;

    const loadMoreBtn = document.getElementById('loadMoreBtn');
    if (loadMoreBtn) {
      loadMoreBtn.addEventListener('click', () => this.loadMore());
    }

    lucide.createIcons();
    if (scrollBottom) this.scrollToBottom();
  },

  _prependMessages(messages) {
    if (messages.length === 0) return;

    const messagesEl = document.getElementById('chatMessages');
    const prevScrollHeight = messagesEl.scrollHeight;

    const oldBtn = document.getElementById('loadMoreBtn');
    if (oldBtn) oldBtn.parentElement.remove();

    const emptyEl = messagesEl.querySelector('.chat-empty');
    if (emptyEl) emptyEl.remove();

    let html = '';
    if (this.hasMore) {
      html += '<div class="load-more-wrapper"><button class="btn btn-ghost btn-sm" id="loadMoreBtn">加载更多</button></div>';
    }

    let lastDate = '';
    messages.forEach(msg => {
      const msgDate = this._getDateLabel(msg.createTime);
      if (msgDate !== lastDate) {
        html += `<div class="date-divider"><span>${msgDate}</span></div>`;
        lastDate = msgDate;
      }
      html += this._renderBubble(msg);
    });

    messagesEl.insertAdjacentHTML('afterbegin', html);

    const newScrollHeight = messagesEl.scrollHeight;
    messagesEl.scrollTop = newScrollHeight - prevScrollHeight;

    const newLoadMoreBtn = document.getElementById('loadMoreBtn');
    if (newLoadMoreBtn) {
      newLoadMoreBtn.addEventListener('click', () => this.loadMore());
    }

    lucide.createIcons();
  },

  _renderBubble(msg) {
    const isSelf = msg.senderId === Auth.getUserId();
    const avatarSrc = Utils.getAvatarUrl(msg.senderAvatar, `user-${msg.senderId}`);
    const time = Utils.formatMessageTime(msg.createTime);
    const escapedContent = Utils.escapeHtml(msg.content);

    let contentHtml = '';
    switch (msg.messageType) {
      case 1:
        contentHtml = `<img src="${msg.fileUrl}" alt="图片" class="message-image" onclick="window.open('${msg.fileUrl}', '_blank')">`;
        break;
      case 2:
        contentHtml = `<a href="${msg.fileUrl}" target="_blank" class="message-file">📎 ${escapedContent}</a>`;
        break;
      case 3:
        contentHtml = `<div class="message-voice"><i data-lucide="mic" style="width:16px;height:16px;"></i> ${escapedContent}</div>`;
        break;
      default:
        contentHtml = `<div class="message-text">${escapedContent}</div>`;
    }

    let readMark = '';
    if (isSelf) {
      readMark = msg.status === 1
        ? '<span class="read-status read">已读</span>'
        : '<span class="read-status unread">未读</span>';
    }

    if (isSelf) {
      return `
        <div class="message self">
          <div class="message-content">
            <div class="message-bubble">${contentHtml}</div>
            <div class="message-meta">${readMark}<span class="message-time">${time}</span></div>
          </div>
        </div>`;
    } else {
      return `
        <div class="message">
          <img src="${avatarSrc}" alt="" class="avatar avatar-sm">
          <div class="message-content">
            <div class="message-bubble">${contentHtml}</div>
            <div class="message-meta"><span class="message-time">${time}</span></div>
          </div>
        </div>`;
    }
  },

  sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    if (!content || !this.currentFriendId) return;

    const sent = WebSocketManager.sendMessage(this.currentFriendId, content, 0);
    if (!sent) {
      API.post('/api/message/private/send', {
        receiverId: this.currentFriendId,
        content: content,
        messageType: 0
      });
    }

    input.value = '';
    input.style.height = 'auto';
    input.focus();
  },

  receiveMessage(msg) {
    const myId = Auth.getUserId();
    const friendId = msg.senderId === myId ? msg.receiverId : msg.senderId;

    ConversationManager.addOrUpdateConversation(msg);

    if (this.currentFriendId === friendId) {
      this._appendBubble(msg);
      this.scrollToBottom();
      if (msg.senderId !== myId) this._markAsRead(friendId);
    }
  },

  receiveReadReceipt(fromUserId) {
    if (this.currentFriendId !== fromUserId) return;
    const messagesEl = document.getElementById('chatMessages');
    messagesEl.querySelectorAll('.read-status.unread').forEach(el => {
      el.className = 'read-status read';
      el.textContent = '已读';
    });
  },

  _appendBubble(msg) {
    const messagesEl = document.getElementById('chatMessages');
    const emptyEl = messagesEl.querySelector('.chat-empty');
    if (emptyEl) emptyEl.remove();

    const msgDate = this._getDateLabel(msg.createTime);
    const lastDivider = messagesEl.querySelector('.date-divider:last-of-type span');
    if (!lastDivider || lastDivider.textContent !== msgDate) {
      messagesEl.insertAdjacentHTML('beforeend', `<div class="date-divider"><span>${msgDate}</span></div>`);
    }

    messagesEl.insertAdjacentHTML('beforeend', this._renderBubble(msg));
    lucide.createIcons();
  },

  loadMore() {
    if (!this.hasMore || !this.currentFriendId) return;
    this.loadHistory(this.currentFriendId, this.currentPage + 1, false);
  },

  _markAsRead(friendId) {
    API.put(`/api/message/private/read?friendId=${friendId}`);
    WebSocketManager.sendReadNotification(friendId);
  },

  scrollToBottom() {
    const messagesEl = document.getElementById('chatMessages');
    requestAnimationFrame(() => {
      messagesEl.scrollTop = messagesEl.scrollHeight;
    });
  },

  _getDateLabel(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const msgDay = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const diff = today - msgDay;

    if (diff === 0) return '今天';
    if (diff === 86400000) return '昨天';
    if (diff < 86400000 * 7) {
      const days = ['日', '一', '二', '三', '四', '五', '六'];
      return '星期' + days[date.getDay()];
    }

    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${month}-${day}`;
  }
};
