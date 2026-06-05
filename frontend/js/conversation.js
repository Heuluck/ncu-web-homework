/**
 * 会话列表管理（支持私聊 + 群聊混合显示）
 */
const ConversationManager = {
  conversations: [],

  async loadConversations() {
    // 加载私聊最近会话
    const res = await API.get('/api/message/private/recent');
    let privateList = [];
    if (res && res.code === 200) {
      privateList = (res.data || []).map(c => ({ ...c, _type: 'private', _id: c.friendId }));
    }

    // 加载群聊最近会话
    let groupList = [];
    if (typeof GroupManager !== 'undefined' && GroupManager.groups) {
      groupList = GroupManager.groups.map(g => ({
        _type: 'group',
        _id: g.groupId,
        friendId: g.groupId,
        nickname: g.groupName,
        avatar: g.groupAvatar,
        lastMessage: g.lastMessage || '',
        lastTime: g.lastTime || null,
        unreadCount: g.unreadCount || 0,
        onlineStatus: 0
      }));
    }

    // 合并并按时间排序
    const merged = [...privateList, ...groupList];
    merged.sort((a, b) => {
      if (!a.lastTime && !b.lastTime) return 0;
      if (!a.lastTime) return 1;
      if (!b.lastTime) return -1;
      return new Date(b.lastTime) - new Date(a.lastTime);
    });

    this.conversations = merged;
    this.renderList();
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
        const id = parseInt(item.dataset.friendId);
        const type = item.dataset.convType || 'private';
        this.selectConversation(id, type);
      });
    });

    lucide.createIcons();
  },

  _renderItem(conv) {
    const isGroup = conv._type === 'group';
    const isActive = isGroup
      ? (typeof GroupManager !== 'undefined' && GroupManager.currentGroupId === conv._id)
      : (ChatManager.currentFriendId === conv.friendId);
    const statusClass = isGroup ? '' : Utils.getStatusClass(conv.onlineStatus);
    const avatarSrc = isGroup
      ? (conv.avatar || '')
      : Utils.getAvatarUrl(conv.avatar, `user-${conv.friendId}`);
    const timeText = conv.lastTime ? Utils.formatTime(conv.lastTime) : '';
    const lastMsg = conv.lastMessageType
      ? conv.lastMessageType
      : Utils.escapeHtml(conv.lastMessage || '');
    const unreadBadge = conv.unreadCount > 0
      ? `<span class="badge">${conv.unreadCount > 99 ? '99+' : conv.unreadCount}</span>`
      : '';
    // 群聊头像：有图用图，无图用群名首字
    const avatarHtml = isGroup && !conv.avatar
      ? `<div class="avatar avatar-md" style="background:var(--color-primary);color:#fff;display:flex;align-items:center;justify-content:center;font-size:16px;font-weight:600;">${Utils.escapeHtml((conv.nickname || '?')[0])}</div>`
      : `<img src="${avatarSrc}" alt="${Utils.escapeHtml(conv.nickname)}" class="avatar avatar-md">`;

    return `
      <div class="list-item${isActive ? ' active' : ''}" data-friend-id="${conv._id}" data-conv-type="${conv._type || 'private'}">
        <div class="avatar-wrapper">
          ${avatarHtml}
          ${!isGroup ? `<span class="status-indicator ${statusClass}"></span>` : ''}
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

  selectConversation(id, type) {
    type = type || 'private';
    document.querySelectorAll('#conversationList .list-item').forEach(item => {
      item.classList.toggle('active', parseInt(item.dataset.friendId) === id);
    });
    if (type === 'group') {
      // 切换到群聊时清除私聊状态
      ChatManager.currentFriendId = null;
      ChatManager.currentFriendInfo = null;
      if (typeof GroupManager !== 'undefined') {
        GroupManager.openGroupChat(id);
      }
    } else {
      // 切换到私聊时清除群聊状态
      if (typeof GroupManager !== 'undefined') {
        GroupManager.currentGroupId = null;
        GroupManager.currentGroupInfo = null;
      }
      // 隐藏群聊专属按钮
      const membersBtn = document.getElementById('groupMembersBtn');
      const settingsBtn = document.getElementById('groupSettingsBtn');
      const callBtn2 = document.getElementById('voiceCallBtn');
      if (membersBtn) membersBtn.style.display = 'none';
      if (settingsBtn) settingsBtn.style.display = 'none';
      if (callBtn2) callBtn2.style.display = 'inline-flex'; // 私聊显示语音通话
      ChatManager.openChat(id);
    }
  },

  addOrUpdateConversation(msg) {
    const myId = Auth.getUserId();
    const friendId = msg.senderId === myId ? msg.receiverId : msg.senderId;
    const existing = this.conversations.find(c => c._type === 'private' && c.friendId === friendId);

    if (existing) {
      existing.lastMessage = msg.content;
      existing.lastMessageType = this._getMessageTypeText(msg.messageType);
      existing.lastTime = msg.createTime;
      if (msg.senderId !== myId && ChatManager.currentFriendId !== friendId) {
        existing.unreadCount = (existing.unreadCount || 0) + 1;
      }
      this.conversations = [existing, ...this.conversations.filter(c => !(c._type === 'private' && c.friendId === friendId))];
    } else {
      this.loadConversations();
      return;
    }
    this.renderList();
  },

  addOrUpdateGroupConversation(msg) {
    const groupId = msg.groupId;
    const existing = this.conversations.find(c => c._type === 'group' && c._id === groupId);

    if (existing) {
      existing.lastMessage = msg.content;
      existing.lastMessageType = this._getMessageTypeText(msg.messageType);
      existing.lastTime = msg.createTime;
      if (msg.senderId !== Auth.getUserId() && GroupManager.currentGroupId !== groupId) {
        existing.unreadCount = (existing.unreadCount || 0) + 1;
      }
      this.conversations = [existing, ...this.conversations.filter(c => !(c._type === 'group' && c._id === groupId))];
    } else {
      // 新群聊会话，重新加载
      this.loadConversations();
      return;
    }
    this.renderList();
  },

  clearUnread(friendId) {
    const conv = this.conversations.find(c => c._type === 'private' && c.friendId === friendId);
    if (conv) {
      conv.unreadCount = 0;
      this.renderList();
    }
  },

  clearGroupUnread(groupId) {
    const conv = this.conversations.find(c => c._type === 'group' && c._id === groupId);
    if (conv) {
      conv.unreadCount = 0;
      this.renderList();
    }
  },

  updateOnlineStatus(userId, status) {
    const conv = this.conversations.find(c => c._type === 'private' && c.friendId === userId);
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
      case 4: return '[语音通话]';
      default: return '';
    }
  }
};
