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

  async openChat(friendId, fallbackInfo) {
    if (this.currentFriendId === friendId) return;

    // 清除群聊状态
    if (typeof GroupManager !== 'undefined') {
      GroupManager.currentGroupId = null;
      GroupManager.currentGroupInfo = null;
    }

    this.currentFriendId = friendId;
    const openedFor = friendId; // 记录本次打开的 ID，防止异步回调时 ID 已变
    this.currentPage = 1;
    this.hasMore = true;

    const conv = ConversationManager.conversations.find(c => c.friendId === friendId);
    this.currentFriendInfo = conv || (fallbackInfo || { friendId, nickname: '加载中...', avatar: null, onlineStatus: 0 });

    this._renderHeader();
    this._updateInputState();
    document.getElementById('chatInputArea').style.display = '';

    const messagesEl = document.getElementById('chatMessages');
    messagesEl.innerHTML = '<div class="chat-loading"><div class="spinner"></div></div>';

    await this.loadHistory(friendId, 1, true);
    // 异步返回后检查是否仍然是当前会话
    if (this.currentFriendId !== openedFor) return;
    this._markAsRead(friendId);
    ConversationManager.clearUnread(friendId);
    ConversationManager.refreshMuteBtn();
  },

  _renderHeader() {
    const info = this.currentFriendInfo;
    if (!info) return;

    // 更新更多按钮为私聊样式
    const moreBtn = document.getElementById('chatMoreBtn');
    if (moreBtn) {
      moreBtn.innerHTML = '<i data-lucide="more-vertical"></i>';
      moreBtn.title = '更多';
      lucide.createIcons({ nodes: [moreBtn] });
    }

    // 如果 blockStatus 未设置，尝试从 FriendManager 查找
    if (!info.blockStatus && typeof FriendManager !== 'undefined' && FriendManager.groups) {
      for (const group of FriendManager.groups) {
        const f = (group.friends || []).find(x => x.friendId === info.friendId);
        if (f && f.blockStatus) { info.blockStatus = f.blockStatus; break; }
      }
    }

    document.getElementById('chatHeader').style.display = '';
    const avatarEl = document.getElementById('chatAvatar');
    avatarEl.src = Utils.getAvatarUrl(info.avatar, `user-${info.friendId}`);
    avatarEl.alt = info.nickname;
    avatarEl.style.display = ''; // 恢复显示（群聊可能隐藏了）
    // 移除群聊字母头像
    const letterAvatar = avatarEl.parentElement?.querySelector('.group-letter-avatar');
    if (letterAvatar) letterAvatar.remove();

    let nameHtml = Utils.escapeHtml(info.nickname || '');
    if (info.blockStatus === 'blocked_by_me') nameHtml += ' <span style="color:var(--color-danger);font-size:12px;">(已拉黑)</span>';
    else if (info.blockStatus === 'blocked_by_them') nameHtml += ' <span style="color:var(--color-warning);font-size:12px;">(被拉黑)</span>';
    else if (info.blockStatus === 'both') nameHtml += ' <span style="color:var(--color-danger);font-size:12px;">(互相拉黑)</span>';
    document.getElementById('chatName').innerHTML = nameHtml;

    const statusEl = document.getElementById('chatStatus');
    statusEl.style.display = '';
    statusEl.className = `status-indicator ${Utils.getStatusClass(info.onlineStatus)}`;
    document.getElementById('chatStatusText').textContent = Utils.getStatusText(info.onlineStatus);
  },

  _updateInputState() {
    const info = this.currentFriendInfo;
    const blocked = info && info.blockStatus && info.blockStatus !== 'none';
    const input = document.getElementById('messageInput');
    const sendBtn = document.getElementById('sendBtn');
    if (input) {
      input.disabled = blocked;
      input.placeholder = blocked ? '无法发送消息（拉黑限制）' : '输入消息...';
    }
    if (sendBtn) {
      sendBtn.disabled = blocked;
    }
  },

  updateHeaderStatus(status) {
    if (!this.currentFriendInfo) return;
    this.currentFriendInfo.onlineStatus = status;
    const statusEl = document.getElementById('chatStatus');
    statusEl.style.display = '';
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

    messages.forEach(msg => {
      html += this._renderBubble(msg);
    });

    messagesEl.innerHTML = html;

    const loadMoreBtn = document.getElementById('loadMoreBtn');
    if (loadMoreBtn) {
      loadMoreBtn.addEventListener('click', () => this.loadMore());
    }

    lucide.createIcons();
    if (scrollBottom) this._scrollAfterImages(messagesEl);
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

    messages.forEach(msg => {
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
    let skipBubble = false;
    const messageType = msg.messageType || 0;
    
    switch (messageType) {
      case 1:
        // 图片消息 - 检查是否是 emoji 表情
        // 先解码 URL 编码的 emoji 字符
        let fileUrl = msg.fileUrl;
        if (fileUrl && fileUrl.startsWith('http://localhost:8080/')) {
          try {
            fileUrl = decodeURIComponent(fileUrl.replace('http://localhost:8080/', ''));
          } catch (e) {
            console.error('解码失败:', e);
          }
        }
        
        if (fileUrl && fileUrl.length <= 10) {
          // 是 emoji 表情，直接显示
          contentHtml = `<span style="font-size: 28px;vertical-align:middle;">${fileUrl}</span>`;
        } else if (fileUrl) {
          // 普通图片 - 点击弹出预览（使用 lightbox）
          skipBubble = true;
          const previewUrl = msg.fileUrl.replace(/'/g, "\\'");
          contentHtml = `<div class="message-image-wrap" onclick="GroupManager.showImagePreview('${previewUrl}')"><img src="${msg.fileUrl}" alt="图片" class="message-image"><div class="image-overlay"><div class="image-overlay-icon"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 3h6v6"/><path d="M10 14 21 3"/><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1-2-2h6"/></svg></div></div></div>`;
        } else {
          contentHtml = `<div class="message-text">${escapedContent}</div>`;
        }
        break;
      case 2:
        skipBubble = true;
        contentHtml = `<a href="${msg.fileUrl}" target="_blank" class="message-file-card"><div class="file-icon"><svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/></svg></div><div class="file-info"><div class="file-name">${escapedContent}</div><div class="file-meta">文件</div></div><div class="file-download"><svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/></svg></div></a>`;
        break;
      case 3:
        contentHtml = (typeof VoiceManager !== 'undefined' && VoiceManager.renderVoiceBubble)
          ? VoiceManager.renderVoiceBubble(msg, isSelf)
          : `<div class="message-voice"><i data-lucide="mic" style="width:16px;height:16px;"></i> ${escapedContent}</div>`;
        break;
      case 4:
        // 通话记录：根据 isSelf 区分双方看到的文案
        // senderId 始终是拨打方，所以 isSelf 意味着"我是拨打方"
        let callDisplay = escapedContent;
        if (escapedContent === '已取消') {
          callDisplay = isSelf ? '已取消' : '对方已取消';
        } else if (escapedContent === '对方已拒绝') {
          callDisplay = isSelf ? '对方已拒绝' : '已拒绝';
        }
        contentHtml = `<div class="message-call"><i data-lucide="phone" style="width:14px;height:14px;"></i> 语音通话 · ${callDisplay}</div>`;
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
            ${skipBubble ? contentHtml : `<div class="message-bubble">${contentHtml}</div>`}
            <div class="message-meta">${readMark}<span class="message-time">${time}</span></div>
          </div>
        </div>`;
    } else {
      return `
        <div class="message">
          <img src="${avatarSrc}" alt="" class="avatar avatar-sm">
          <div class="message-content">
            ${skipBubble ? contentHtml : `<div class="message-bubble">${contentHtml}</div>`}
            <div class="message-meta"><span class="message-time">${time}</span></div>
          </div>
        </div>`;
    }
  },

  async sendMessage(content, messageType = 0, fileUrl = null) {
    if (!this.currentFriendId) return;

    // 拉黑状态检查
    if (this.currentFriendInfo && this.currentFriendInfo.blockStatus && this.currentFriendInfo.blockStatus !== 'none') {
      Utils.showToast('由于拉黑限制，无法发送消息', 'error');
      return;
    }

    const sent = WebSocketManager.sendMessage(this.currentFriendId, content, messageType, fileUrl);
    if (!sent) {
      const res = await API.post('/api/message/private/send', {
        receiverId: this.currentFriendId,
        content: content,
        messageType: messageType,
        fileUrl: fileUrl
      });
      if (res && res.code !== 200) {
        Utils.showToast(res?.message || '发送失败', 'error');
        return;
      }
    }
  },

  sendTextMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    if (!content || !this.currentFriendId) return;

    this.sendMessage(content, 0, null);

    input.value = '';
    input.style.height = 'auto';
    input.focus();
  },

  /**
   * 发送图片消息
   */
  async sendImage(file) {
    if (!this.currentFriendId) return;

    // 上传文件
    const uploadResult = await API.upload(file);
    if (!uploadResult || uploadResult.code !== 200) {
      Utils.showToast('图片上传失败', 'error');
      return;
    }

    const fileUrl = uploadResult.data.url;
    // 发送图片消息，内容为文件名
    this.sendMessage(file.name || '图片', 1, fileUrl);
  },

  /**
   * 发送文件消息
   */
  async sendFile(file) {
    if (!this.currentFriendId) return;

    // 上传文件
    const uploadResult = await API.upload(file);
    if (!uploadResult || uploadResult.code !== 200) {
      Utils.showToast('文件上传失败', 'error');
      return;
    }

    const fileUrl = uploadResult.data.url;
    // 发送文件消息，内容为文件名
    this.sendMessage(file.name || '文件', 2, fileUrl);
  },

  receiveMessage(msg) {
    // 去重：防止同一条消息渲染两次
    if (msg.id && this._renderedMsgIds && this._renderedMsgIds.has(msg.id)) return;
    if (!this._renderedMsgIds) this._renderedMsgIds = new Set();
    if (msg.id) this._renderedMsgIds.add(msg.id);

    const myId = Auth.getUserId();
    const friendId = msg.senderId === myId ? msg.receiverId : msg.senderId;

    // 语音消息的会话预览显示为 [语音]
    const convMsg = { ...msg };
    if (convMsg.messageType === 3) convMsg.content = '[语音]';
    ConversationManager.addOrUpdateConversation(convMsg);

    if (this.currentFriendId === friendId) {
      this._appendBubble(msg);
      this.scrollToBottom();
      if (msg.senderId !== myId) {
        // 只在页面聚焦且可见时才标记已读
        if (document.hasFocus() && !document.hidden) {
          this._markAsRead(friendId);
        } else {
          // 等待页面重新聚焦时标记已读
          this._pendingReadFriendId = friendId;
        }
      }
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

    messagesEl.insertAdjacentHTML('beforeend', this._renderBubble(msg));
    lucide.createIcons();

    // 图片消息：等图片加载完成后再置底
    const img = messagesEl.querySelector('.message:last-child .message-image');
    if (img) {
      if (img.complete) {
        this.scrollToBottom();
      } else {
        img.onload = () => this.scrollToBottom();
      }
    }
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
    if (!messagesEl) return;
    requestAnimationFrame(() => {
      messagesEl.scrollTop = messagesEl.scrollHeight;
    });
  },

  /**
   * 先立即滚底，再监听每张图片加载完成后修正位置
   */
  _scrollAfterImages(container) {
    this.scrollToBottom();
    const images = container.querySelectorAll('.message-image');
    images.forEach(img => {
      if (img.complete) return;
      const onDone = () => this.scrollToBottom();
      img.addEventListener('load', onDone, { once: true });
      img.addEventListener('error', onDone, { once: true });
    });
  },

  showChatMenu(event) {
    event.preventDefault();
    event.stopPropagation();
    this._closeChatMenu();

    // 群聊模式不显示好友菜单
    if (GroupManager.currentGroupId) return;

    const friendId = this.currentFriendId;
    if (!friendId) return;

    // 从 FriendManager 查找 friendshipId 和 blockStatus
    let friendshipId = null;
    let blockStatus = 'none';
    if (typeof FriendManager !== 'undefined' && FriendManager.groups) {
      for (const group of FriendManager.groups) {
        const f = (group.friends || []).find(x => x.friendId === friendId);
        if (f) {
          friendshipId = f.friendshipId;
          blockStatus = f.blockStatus || 'none';
          break;
        }
      }
    }

    const canUnblock = blockStatus === 'blocked_by_me' || blockStatus === 'both';
    const canBlock = blockStatus === 'none' || blockStatus === 'blocked_by_them';

    const menu = document.createElement('div');
    menu.id = 'chatMoreMenu';
    menu.className = 'context-menu';
    menu.style.cssText = `position:fixed;left:${event.clientX - 180}px;top:${event.clientY + 8}px;z-index:2000;`;

    let items = `<div class="context-menu-item" onclick="FriendManager.currentContextFriendId=${friendId};FriendManager._ctxViewProfile();FriendManager._closeContextMenu();ChatManager._closeChatMenu();">
          <i data-lucide="user" style="width:16px;height:16px;"></i> 查看资料
        </div>`;
    if (!friendshipId) {
      items += `<div class="context-divider"></div>
        <div class="context-menu-item" style="color:var(--text-muted);cursor:default;">还不是好友</div>`;
    } else {
      items += `<div class="context-menu-item" onclick="FriendManager.openMoveFriendModal(${friendshipId});ChatManager._closeChatMenu();">
          <i data-lucide="folder-input" style="width:16px;height:16px;"></i> 移动到分组
        </div>`;
      if (canUnblock) {
        items += `<div class="context-divider"></div>
          <div class="context-menu-item" onclick="FriendManager.currentContextFriendshipId=${friendshipId};FriendManager._ctxUnblock();ChatManager._closeChatMenu();" style="color:var(--color-primary);">取消拉黑</div>`;
      }
      if (canBlock) {
        items += `<div class="context-divider"></div>
          <div class="context-menu-item" onclick="FriendManager.currentContextFriendshipId=${friendshipId};FriendManager._ctxBlock();ChatManager._closeChatMenu();" style="color:var(--color-danger);">拉黑</div>`;
      }
      items += `<div class="context-menu-item" onclick="FriendManager.currentContextFriendshipId=${friendshipId};FriendManager._ctxDelete();ChatManager._closeChatMenu();" style="color:var(--color-danger);">删除好友</div>`;
    }

    menu.innerHTML = `<div style="background:var(--bg-app);border:1px solid var(--border-subtle);border-radius:var(--radius-lg);box-shadow:var(--shadow-lg);min-width:170px;padding:4px 0;">${items}</div>`;

    document.body.appendChild(menu);
    lucide.createIcons();

    // 调整位置防止溢出
    const rect = menu.getBoundingClientRect();
    if (rect.right > window.innerWidth) menu.style.left = (window.innerWidth - rect.width - 8) + 'px';
    if (rect.bottom > window.innerHeight) menu.style.top = (window.innerHeight - rect.height - 8) + 'px';

    setTimeout(() => {
      document.addEventListener('click', this._closeChatMenu, { once: true });
    }, 0);
  },

  _closeChatMenu() {
    document.getElementById('chatMoreMenu')?.remove();
  },

};
