/**
 * 会话列表管理（支持私聊 + 群聊混合显示）
 */
const ConversationManager = {
  conversations: [],

  /** 免打扰存储 key -> localStorage */
  _mutedKey: 'chat_muted',

  _loadMuted() {
    try { return new Set(JSON.parse(localStorage.getItem(this._mutedKey) || '[]')); }
    catch (_) { return new Set(); }
  },

  _saveMuted(s) {
    localStorage.setItem(this._mutedKey, JSON.stringify([...s]));
  },

  _muteKey(type, id) { return `${type}:${id}`; },

  isMuted(type, id) { return this._loadMuted().has(this._muteKey(type, id)); },

  toggleMute(type, id) {
    const s = this._loadMuted();
    const key = this._muteKey(type, id);
    s.has(key) ? s.delete(key) : s.add(key);
    this._saveMuted(s);
    this.renderList();
    return !s.has(key); // 返回操作后的状态：true=已免打扰，false=已取消
  },

  /** 当前打开的会话类型 */
  currentConvType() {
    if (typeof GroupManager !== 'undefined' && GroupManager.currentGroupId) return 'group';
    if (ChatManager.currentFriendId) return 'private';
    return null;
  },

  /** 当前打开的会话 ID */
  currentConvId() {
    if (typeof GroupManager !== 'undefined' && GroupManager.currentGroupId) return GroupManager.currentGroupId;
    if (ChatManager.currentFriendId) return ChatManager.currentFriendId;
    return null;
  },

  /** 刷新头部免打扰按钮状态 */
  refreshMuteBtn() {
    const btn = document.getElementById('muteBtn');
    if (!btn) return;
    const type = this.currentConvType();
    const id = this.currentConvId();
    if (!type || !id) { btn.style.display = 'none'; return; }
    btn.style.display = 'inline-flex';
    const muted = this.isMuted(type, id);
    btn.innerHTML = muted ? '<i data-lucide="bell-off"></i>' : '<i data-lucide="bell"></i>';
    btn.title = muted ? '取消免打扰' : '消息免打扰';
    lucide.createIcons();
  },

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
        lastMessageType: g.lastMessageType || '',
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

    // 有未读且未免打扰的会话 → "未读消息"分组
    const unreadActive = this.conversations.filter(c =>
      c.unreadCount > 0 && !this.isMuted(c._type, c._id)
    );
    // 免打扰的会话（无论有没有未读，不显示在"未读消息"区）
    const muted = this.conversations.filter(c => this.isMuted(c._type, c._id));
    // 已读且未免打扰的会话
    const readNormal = this.conversations.filter(c =>
      c.unreadCount === 0 && !this.isMuted(c._type, c._id)
    );

    let html = '';
    if (unreadActive.length > 0) {
      html += '<div class="list-group-title">未读消息</div>';
      unreadActive.forEach(c => { html += this._renderItem(c); });
    }
    if (muted.length > 0) {
      html += '<div class="list-group-title">免打扰</div>';
      muted.forEach(c => { html += this._renderItem(c); });
    }
    if (readNormal.length > 0) {
      html += '<div class="list-group-title">全部会话</div>';
      readNormal.forEach(c => { html += this._renderItem(c); });
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
      ? `<span class="badge${this.isMuted(conv._type, conv._id) ? ' badge-muted' : ''}">${conv.unreadCount > 99 ? '99+' : conv.unreadCount}</span>`
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
    this.refreshMuteBtn();
  },

  addOrUpdateConversation(msg) {
    const myId = Auth.getUserId();
    const friendId = msg.senderId === myId ? msg.receiverId : msg.senderId;
    const existing = this.conversations.find(c => c._type === 'private' && c.friendId === friendId);

    // 判断是否是 emoji 表情（messageType=1 且 fileUrl 长度 <= 10）
    const isEmoji = msg.messageType === 1 && msg.fileUrl && msg.fileUrl.length <= 10;
    const previewContent = isEmoji ? msg.fileUrl : msg.content;
    const previewType = isEmoji ? '' : this._getMessageTypeText(msg.messageType);

    if (existing) {
      existing.lastMessage = previewContent;
      existing.lastMessageType = previewType;
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

    // 判断是否是 emoji 表情（messageType=1 且 fileUrl 长度 <= 10）
    const isEmoji = msg.messageType === 1 && msg.fileUrl && msg.fileUrl.length <= 10;
    const previewContent = isEmoji ? msg.fileUrl : msg.content;
    const previewType = isEmoji ? '' : this._getMessageTypeText(msg.messageType);

    if (existing) {
      existing.lastMessage = previewContent;
      existing.lastMessageType = previewType;
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
