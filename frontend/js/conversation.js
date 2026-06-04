/**
 * 会话列表管理
 */
const ConversationManager = {
  conversations: [],

  async loadConversations() {
    const res = await API.get('/api/message/private/recent');
    if (res && res.code === 200) {
      this.conversations = res.data || [];
      this.renderList();
    }
  },

  renderList() {
    const container = document.getElementById('conversationList');
    if (!container) return;

    if (this.conversations.length === 0) {
      container.innerHTML = `
        <div class="empty-state" style="padding: 40px 20px;">
          <div class="empty-state-icon">
            <i data-lucide="message-square" style="width: 40px; height: 40px;"></i>
          </div>
          <div class="empty-state-title" style="font-size: 14px;">暂无会话</div>
          <div class="empty-state-text" style="font-size: 12px;">找到好友开始聊天吧</div>
        </div>`;
      lucide.createIcons();
      return;
    }

    const unread = this.conversations.filter(c => c.unreadCount > 0);
    const read = this.conversations.filter(c => c.unreadCount === 0);

    let html = '';
    if (unread.length > 0) {
      html += '<div class="list-group-title">未读消息</div>';
      unread.forEach(c => { html += this._renderItem(c); });
    }
    if (read.length > 0) {
      if (unread.length > 0) html += '<div class="list-group-title">全部会话</div>';
      read.forEach(c => { html += this._renderItem(c); });
    }

    container.innerHTML = html;

    container.querySelectorAll('.list-item').forEach(item => {
      item.addEventListener('click', () => {
        const friendId = parseInt(item.dataset.friendId);
        this.selectConversation(friendId);
      });
    });

    lucide.createIcons();
  },

  _renderItem(conv) {
    const isActive = ChatManager.currentFriendId === conv.friendId;
    const statusClass = Utils.getStatusClass(conv.onlineStatus);
    const avatarSrc = Utils.getAvatarUrl(conv.avatar, `user-${conv.friendId}`);
    const timeText = conv.lastTime ? Utils.formatTime(conv.lastTime) : '';
    const lastMsg = conv.lastMessageType
      ? conv.lastMessageType
      : Utils.escapeHtml(conv.lastMessage || '');
    const unreadBadge = conv.unreadCount > 0
      ? `<span class="badge">${conv.unreadCount > 99 ? '99+' : conv.unreadCount}</span>`
      : '';

    return `
      <div class="list-item${isActive ? ' active' : ''}" data-friend-id="${conv.friendId}">
        <div class="avatar-wrapper">
          <img src="${avatarSrc}" alt="${Utils.escapeHtml(conv.nickname)}" class="avatar avatar-md">
          <span class="status-indicator ${statusClass}"></span>
        </div>
        <div class="list-item-content">
          <div class="list-item-header">
            <span class="list-item-title">${Utils.escapeHtml(conv.nickname)}</span>
            <span class="list-item-time">${timeText}</span>
          </div>
          <div class="list-item-preview">
            <span class="text-truncate">${lastMsg}</span>
            ${unreadBadge}
          </div>
        </div>
      </div>`;
  },

  selectConversation(friendId) {
    document.querySelectorAll('#conversationList .list-item').forEach(item => {
      item.classList.toggle('active', parseInt(item.dataset.friendId) === friendId);
    });
    ChatManager.openChat(friendId);
  },

  addOrUpdateConversation(msg) {
    const myId = Auth.getUserId();
    const friendId = msg.senderId === myId ? msg.receiverId : msg.senderId;
    const existing = this.conversations.find(c => c.friendId === friendId);

    if (existing) {
      existing.lastMessage = msg.content;
      existing.lastMessageType = this._getMessageTypeText(msg.messageType);
      existing.lastTime = msg.createTime;
      if (msg.senderId !== myId && ChatManager.currentFriendId !== friendId) {
        existing.unreadCount = (existing.unreadCount || 0) + 1;
      }
      this.conversations = [existing, ...this.conversations.filter(c => c.friendId !== friendId)];
    } else {
      this.loadConversations();
      return;
    }
    this.renderList();
  },

  clearUnread(friendId) {
    const conv = this.conversations.find(c => c.friendId === friendId);
    if (conv) {
      conv.unreadCount = 0;
      this.renderList();
    }
  },

  updateOnlineStatus(userId, status) {
    const conv = this.conversations.find(c => c.friendId === userId);
    if (conv) {
      conv.onlineStatus = status;
      this.renderList();
    }
    if (ChatManager.currentFriendId === userId) {
      ChatManager.updateHeaderStatus(status);
    }
  },

  _getMessageTypeText(type) {
    switch (type) {
      case 1: return '[图片]';
      case 2: return '[文件]';
      case 3: return '[语音]';
      default: return '';
    }
  }
};
