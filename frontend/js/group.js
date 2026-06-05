/**
 * 群聊管理器
 */
const GroupManager = {
    currentGroupId: null,
    currentGroupInfo: null,
    groups: [],
    members: [],
    currentPage: 1,
    pageSize: 20,
    hasMore: true,
    loading: false,

    // 加载我的群列表
    async loadMyGroups() {
        const res = await API.get('/api/group/my-groups');
        if (res && res.code === 200) {
            this.groups = res.data || [];
            this.renderGroupList();
        }
    },

    // 渲染群列表（只更新 groupListContainer，不影响创建按钮）
    renderGroupList() {
        const container = document.getElementById('groupListContainer');
        if (!container) return;

        if (this.groups.length === 0) {
            container.innerHTML = `
                <div class="empty-state" style="padding: 40px 20px; text-align: center;">
                    <div class="empty-state-icon">
                        <i data-lucide="users" style="width: 40px; height: 40px;"></i>
                    </div>
                    <div class="empty-state-title">暂无群聊</div>
                    <div class="empty-state-text">点击上方按钮创建群聊</div>
                </div>`;
            lucide.createIcons();
            return;
        }

        let html = '';
        for (const group of this.groups) {
            const isActive = this.currentGroupId === group.groupId;
            const avatarSrc = Utils.getAvatarUrl(group.groupAvatar, `group-${group.groupId}`);
            const unreadBadge = group.unreadCount > 0
                ? `<span class="badge">${group.unreadCount > 99 ? '99+' : group.unreadCount}</span>`
                : '';

            html += `
                <div class="list-item ${isActive ? 'active' : ''}" data-group-id="${group.groupId}">
                    <div class="avatar-wrapper">
                        <img src="${avatarSrc}" class="avatar avatar-md">
                    </div>
                    <div class="list-item-content">
                        <div class="list-item-header">
                            <span class="list-item-title">${Utils.escapeHtml(group.groupName)}</span>
                        </div>
                        <div class="list-item-preview">
                            <span class="text-truncate">${Utils.escapeHtml(group.lastMessage || '')}</span>
                            ${unreadBadge}
                        </div>
                    </div>
                </div>`;
        }
        container.innerHTML = html;

        container.querySelectorAll('.list-item').forEach(item => {
            item.addEventListener('click', () => {
                const groupId = parseInt(item.dataset.groupId);
                this.openGroupChat(groupId);
            });
        });
        lucide.createIcons();
    },

    // 打开群聊窗口
    async openGroupChat(groupId) {
        if (this.currentGroupId === groupId) return;
        this.currentGroupId = groupId;
        this.currentPage = 1;
        this.hasMore = true;

        // 显示群聊头部按钮
        const membersBtn = document.getElementById('groupMembersBtn');
        const settingsBtn = document.getElementById('groupSettingsBtn');
        if (membersBtn) membersBtn.style.display = 'inline-flex';
        if (settingsBtn) settingsBtn.style.display = 'inline-flex';

        const infoRes = await API.get(`/api/group/info/${groupId}`);
        if (infoRes && infoRes.code === 200) {
            this.currentGroupInfo = infoRes.data;
            this._renderGroupHeader();
        }

        document.getElementById('chatInputArea').style.display = '';
        const messagesEl = document.getElementById('chatMessages');
        messagesEl.innerHTML = '<div class="chat-loading"><div class="spinner"></div></div>';

        await this.loadGroupMessages(groupId, 1, true);
        this._markGroupRead(groupId);
        this._clearGroupUnread(groupId);
    },

    _renderGroupHeader() {
        if (!this.currentGroupInfo) return;
        document.getElementById('chatHeader').style.display = '';
        document.getElementById('chatAvatar').src = Utils.getAvatarUrl(this.currentGroupInfo.avatar, `group-${this.currentGroupInfo.id}`);
        document.getElementById('chatName').textContent = this.currentGroupInfo.name;
        document.getElementById('chatStatusText').textContent = `${this.currentGroupInfo.memberCount || 0} 人`;
    },

    async loadGroupMessages(groupId, pageNum, replace = false) {
        if (this.loading) return;
        this.loading = true;
        const res = await API.get(`/api/group/${groupId}/messages?pageNum=${pageNum}&pageSize=${this.pageSize}`);
        this.loading = false;

        if (res && res.code === 200) {
            const messages = res.data.records || [];
            if (messages.length < this.pageSize) this.hasMore = false;
            this.currentPage = pageNum;
            if (replace) {
                this._renderGroupMessages(messages, true);
            } else {
                this._prependGroupMessages(messages);
            }
        }
    },

    _renderGroupMessages(messages, scrollBottom = false) {
        const messagesEl = document.getElementById('chatMessages');
        const myId = Auth.getUserId();

        if (messages.length === 0 && this.currentPage === 1) {
            messagesEl.innerHTML = `<div class="chat-empty"><div class="empty-state"><div class="empty-state-icon"><i data-lucide="message-square"></i></div><div class="empty-state-title">开始聊天吧</div></div></div>`;
            lucide.createIcons();
            return;
        }

        let html = '';
        if (this.hasMore) html += '<div class="load-more-wrapper"><button class="btn btn-ghost btn-sm" id="loadMoreGroupBtn">加载更多</button></div>';
        messages.forEach(msg => { html += this._renderGroupBubble(msg, myId); });
        messagesEl.innerHTML = html;

        const loadMoreBtn = document.getElementById('loadMoreGroupBtn');
        if (loadMoreBtn) loadMoreBtn.addEventListener('click', () => this.loadMoreGroupMessages());
        lucide.createIcons();
        if (scrollBottom) this.scrollToBottom();
    },

    _prependGroupMessages(messages) {
        if (messages.length === 0) return;
        const messagesEl = document.getElementById('chatMessages');
        const prevScrollHeight = messagesEl.scrollHeight;
        const oldBtn = document.getElementById('loadMoreGroupBtn');
        if (oldBtn) oldBtn.parentElement?.remove();

        let html = '';
        if (this.hasMore) html += '<div class="load-more-wrapper"><button class="btn btn-ghost btn-sm" id="loadMoreGroupBtn">加载更多</button></div>';
        messages.forEach(msg => { html += this._renderGroupBubble(msg, Auth.getUserId()); });
        messagesEl.insertAdjacentHTML('afterbegin', html);
        messagesEl.scrollTop = messagesEl.scrollHeight - prevScrollHeight;

        const newBtn = document.getElementById('loadMoreGroupBtn');
        if (newBtn) newBtn.addEventListener('click', () => this.loadMoreGroupMessages());
        lucide.createIcons();
    },

    _renderGroupBubble(msg, myId) {
        const isSelf = msg.senderId === myId;
        const avatarSrc = Utils.getAvatarUrl(msg.senderAvatar, `user-${msg.senderId}`);
        const time = Utils.formatMessageTime(msg.createTime);

        // 根据消息类型渲染不同内容
        let contentHtml = '';
        const messageType = msg.messageType || 0;

        if (messageType === 1) {
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
                // 普通图片 - 点击弹出预览
                contentHtml = `<img src="${msg.fileUrl}" alt="图片" class="message-image" onclick="GroupManager.showImagePreview('${msg.fileUrl}')">`;
            } else {
                contentHtml = `<div class="message-text">${Utils.escapeHtml(msg.content)}</div>`;
            }
        } else if (messageType === 2) {
            // 文件消息
            contentHtml = `<a href="${msg.fileUrl}" target="_blank" class="message-file">📎 ${Utils.escapeHtml(msg.content)}</a>`;
        } else if (messageType === 3) {
            // 语音消息
            contentHtml = `<div class="message-voice"><i data-lucide="mic" style="width:16px;height:16px;"></i> ${Utils.escapeHtml(msg.content)}</div>`;
        } else {
            // 文字消息
            contentHtml = `<div class="message-text">${Utils.escapeHtml(msg.content)}</div>`;
        }

        if (isSelf) {
            return `<div class="message self"><div class="message-content"><div class="message-bubble">${contentHtml}</div><div class="message-meta"><span class="message-time">${time}</span></div></div></div>`;
        } else {
            return `<div class="message"><img src="${avatarSrc}" class="avatar avatar-sm"><div class="message-content"><div class="message-header"><span class="message-sender">${Utils.escapeHtml(msg.senderNickname)}</span><span class="message-time">${time}</span></div><div class="message-bubble">${contentHtml}</div></div></div>`;
        }
    },

    loadMoreGroupMessages() {
        if (!this.hasMore || !this.currentGroupId) return;
        this.loadGroupMessages(this.currentGroupId, this.currentPage + 1, false);
    },

    sendGroupMessage(content, messageType = 0, fileUrl = null) {
        const input = document.getElementById('messageInput');
        if (!content) {
            content = input?.value.trim() || '';
        }
        
        if (!content || !this.currentGroupId) return;

        // 检查是否包含表情标记 [emoji:名称:URL]
        const emojiMatch = content.match(/\[emoji:([^\]]+):([^\]]+)\]/);
        if (emojiMatch) {
            // 是表情，发送表情 URL
            content = emojiMatch[1]; // 表情名称
            messageType = 1; // 图片类型
            fileUrl = emojiMatch[2]; // 表情 URL
        }

        // 清空输入框
        if (input) {
            input.value = '';
            input.style.height = 'auto';
        }

        // 只使用 HTTP API 发送，后端会自动通过 WebSocket 广播
        API.post('/api/group/message/send', {
            groupId: this.currentGroupId,
            content: content,
            messageType: messageType,
            fileUrl: fileUrl
        }).then(res => {
            if (res && res.code === 200) {
                // 发送成功，后端会广播，前端通过 WebSocket 接收
                // 不需要手动添加消息
            } else {
                Utils.showToast(res?.message || '发送失败', 'error');
            }
        });
    },

    /**
     * 发送群图片消息
     */
    async sendGroupImage(file) {
        if (!this.currentGroupId) return;

        console.log('[图片] 开始上传:', file.name);

        // 上传文件
        const uploadResult = await API.upload(file);
        console.log('[图片] 上传结果:', uploadResult);
        
        if (!uploadResult || uploadResult.code !== 200) {
            alert('图片上传失败');
            return;
        }

        const fileUrl = uploadResult.data?.url;
        console.log('[图片] fileUrl:', fileUrl);
        
        if (!fileUrl) {
            alert('上传成功但未返回 URL');
            return;
        }
        
        // 发送图片消息
        console.log('[图片] 发送消息:', { groupId: this.currentGroupId, content: file.name || '图片', messageType: 1, fileUrl: fileUrl });
        this.sendGroupMessage(file.name || '图片', 1, fileUrl);
    },

    /**
     * 发送群文件消息
     */
    async sendGroupFile(file) {
        if (!this.currentGroupId) return;

        // 上传文件
        const uploadResult = await API.upload(file);
        if (!uploadResult || uploadResult.code !== 200) {
            alert('文件上传失败');
            return;
        }

        const fileUrl = uploadResult.data.url;
        // 发送文件消息
        this.sendGroupMessage(file.name || '文件', 2, fileUrl);
    },

    receiveGroupMessage(msg) {
        console.log('[群聊] 收到消息:', msg);
        console.log('[群聊] messageType:', msg.messageType, 'fileUrl:', msg.fileUrl, 'content:', msg.content);
        if (this.currentGroupId === msg.groupId) {
            this._appendGroupBubble(msg);
            this.scrollToBottom();
        }
        this._updateGroupInList(msg);
    },

    _appendGroupBubble(msg) {
        const messagesEl = document.getElementById('chatMessages');
        const emptyEl = messagesEl.querySelector('.chat-empty');
        if (emptyEl) emptyEl.remove();
        messagesEl.insertAdjacentHTML('beforeend', this._renderGroupBubble(msg, Auth.getUserId()));
        lucide.createIcons();
    },

    _updateGroupInList(msg) {
        const group = this.groups.find(g => g.groupId === msg.groupId);
        if (group) {
            group.lastMessage = msg.content;
            group.lastTime = msg.createTime;
            if (msg.senderId !== Auth.getUserId() && this.currentGroupId !== msg.groupId) {
                group.unreadCount = (group.unreadCount || 0) + 1;
            }
            this.groups = [group, ...this.groups.filter(g => g.groupId !== msg.groupId)];
            this.renderGroupList();
        } else {
            this.loadMyGroups();
        }
    },

    _clearGroupUnread(groupId) {
        const group = this.groups.find(g => g.groupId === groupId);
        if (group) group.unreadCount = 0;
        this.renderGroupList();
    },

    _markGroupRead(groupId) {
        API.put(`/api/group/${groupId}/read`);
        if (WebSocketManager && WebSocketManager.sendGroupReadNotification) {
            WebSocketManager.sendGroupReadNotification(groupId);
        }
    },

    scrollToBottom() {
        const messagesEl = document.getElementById('chatMessages');
        requestAnimationFrame(() => messagesEl.scrollTop = messagesEl.scrollHeight);
    },

    showGroupMembers() {
        if (!this.currentGroupId) return;
        if (typeof GroupMemberPanel !== 'undefined') {
            GroupMemberPanel.show(this.currentGroupId, this.currentGroupInfo?.myRole || 0);
        }
    },

    showGroupSettings() {
        if (!this.currentGroupId) return;
        if (typeof GroupSettingsModal !== 'undefined') {
            GroupSettingsModal.show(this.currentGroupId);
        }
    },

    /**
     * 显示图片预览模态框（增强版：支持缩放、拖拽、旋转）
     */
    showImagePreview(imageUrl) {
        let modal = document.getElementById('imagePreviewModal');
        if (!modal) {
            // 创建模态框
            modal = document.createElement('div');
            modal.id = 'imagePreviewModal';
            modal.className = 'image-preview-modal';
            modal.innerHTML = `
                <div class="image-preview-overlay" onclick="GroupManager.closeImagePreview()"></div>
                <div class="image-preview-toolbar">
                    <button class="image-preview-tool" onclick="GroupManager.zoomOut()" title="缩小">
                        <i data-lucide="minus"></i>
                    </button>
                    <span class="image-preview-scale">100%</span>
                    <button class="image-preview-tool" onclick="GroupManager.zoomIn()" title="放大">
                        <i data-lucide="plus"></i>
                    </button>
                    <button class="image-preview-tool" onclick="GroupManager.rotateLeft()" title="向左旋转">
                        <i data-lucide="rotate-ccw"></i>
                    </button>
                    <button class="image-preview-tool" onclick="GroupManager.rotateRight()" title="向右旋转">
                        <i data-lucide="rotate-cw"></i>
                    </button>
                    <button class="image-preview-tool" onclick="GroupManager.resetImage()" title="重置">
                        <i data-lucide="refresh-ccw"></i>
                    </button>
                    <button class="image-preview-tool" onclick="GroupManager.downloadImage()" title="下载">
                        <i data-lucide="download"></i>
                    </button>
                    <button class="image-preview-tool image-preview-close" onclick="GroupManager.closeImagePreview()" title="关闭">
                        <i data-lucide="x"></i>
                    </button>
                </div>
                <div class="image-preview-container">
                    <img src="" alt="预览" class="image-preview-img" draggable="false">
                </div>
            `;
            document.body.appendChild(modal);
            lucide.createIcons();
            
            // 初始化拖拽
            this.initImageDrag();
        }
        
        const img = modal.querySelector('.image-preview-img');
        img.src = imageUrl;
        
        // 重置状态
        this.imageState = {
            scale: 1,
            rotation: 0,
            panX: 0,
            panY: 0,
            isDragging: false,
            startX: 0,
            startY: 0
        };
        
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
        this.updateImageTransform();
    },

    /**
     * 关闭图片预览
     */
    closeImagePreview() {
        const modal = document.getElementById('imagePreviewModal');
        if (modal) {
            modal.classList.remove('active');
            document.body.style.overflow = '';
        }
    },

    /**
     * 初始化图片拖拽
     */
    initImageDrag() {
        const modal = document.getElementById('imagePreviewModal');
        if (!modal) return;
        
        const img = modal.querySelector('.image-preview-img');
        const container = modal.querySelector('.image-preview-container');
        
        container.addEventListener('mousedown', (e) => {
            if (e.target !== img) return;
            this.imageState.isDragging = true;
            this.imageState.startX = e.clientX - this.imageState.panX;
            this.imageState.startY = e.clientY - this.imageState.panY;
            img.style.cursor = 'grabbing';
        });
        
        document.addEventListener('mousemove', (e) => {
            if (!this.imageState.isDragging) return;
            e.preventDefault();
            this.imageState.panX = e.clientX - this.imageState.startX;
            this.imageState.panY = e.clientY - this.imageState.startY;
            this.updateImageTransform();
        });
        
        document.addEventListener('mouseup', () => {
            if (this.imageState.isDragging) {
                this.imageState.isDragging = false;
                img.style.cursor = 'grab';
            }
        });
        
        // 鼠标滚轮缩放
        container.addEventListener('wheel', (e) => {
            e.preventDefault();
            if (e.deltaY < 0) {
                this.zoomIn();
            } else {
                this.zoomOut();
            }
        });
    },

    /**
     * 更新图片变换
     */
    updateImageTransform() {
        const modal = document.getElementById('imagePreviewModal');
        if (!modal) return;
        
        const img = modal.querySelector('.image-preview-img');
        const scaleEl = modal.querySelector('.image-preview-scale');
        
        img.style.transform = `translate(${this.imageState.panX}px, ${this.imageState.panY}px) scale(${this.imageState.scale}) rotate(${this.imageState.rotation}deg)`;
        
        if (scaleEl) {
            scaleEl.textContent = Math.round(this.imageState.scale * 100) + '%';
        }
    },

    /**
     * 放大
     */
    zoomIn() {
        if (this.imageState.scale >= 5) return;
        this.imageState.scale += 0.25;
        this.updateImageTransform();
    },

    /**
     * 缩小
     */
    zoomOut() {
        if (this.imageState.scale <= 0.25) return;
        this.imageState.scale -= 0.25;
        this.updateImageTransform();
    },

    /**
     * 向左旋转
     */
    rotateLeft() {
        this.imageState.rotation -= 90;
        this.updateImageTransform();
    },

    /**
     * 向右旋转
     */
    rotateRight() {
        this.imageState.rotation += 90;
        this.updateImageTransform();
    },

    /**
     * 重置图片
     */
    resetImage() {
        this.imageState = {
            scale: 1,
            rotation: 0,
            panX: 0,
            panY: 0,
            isDragging: false,
            startX: 0,
            startY: 0
        };
        this.updateImageTransform();
    },

    /**
     * 下载图片
     */
    downloadImage() {
        const modal = document.getElementById('imagePreviewModal');
        if (!modal) return;
        
        const img = modal.querySelector('.image-preview-img');
        const link = document.createElement('a');
        link.href = img.src;
        link.download = 'image_' + Date.now() + '.jpg';
        link.target = '_blank';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
};