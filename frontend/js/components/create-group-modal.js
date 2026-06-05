const CreateGroupModal = {
    show(onSuccess) {
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.id = 'createGroupModal';
        modal.innerHTML = `
        <div class="modal-container" style="max-width:560px;">
            <div class="modal-header">
                <h3>创建群聊</h3>
                <button class="modal-close btn-icon"><i data-lucide="x"></i></button>
            </div>
            <div class="modal-body" style="padding:0;">
                <!-- 群名称+公告 -->
                <div style="padding:var(--space-4) var(--space-6);border-bottom:1px solid var(--border-subtle);">
                    <div class="form-group" style="margin-bottom:12px;">
                        <label class="form-label">群名称</label>
                        <input type="text" id="groupName" class="input-control" placeholder="输入群名称">
                    </div>
                    <div class="form-group" style="margin-bottom:0;">
                        <label class="form-label">群公告</label>
                        <textarea id="groupAnnouncement" class="input-control" rows="2" placeholder="群公告（可选）" style="resize:none;"></textarea>
                    </div>
                </div>
                <!-- 邀请好友：左右结构 -->
                <div style="padding:var(--space-4) var(--space-6);">
                    <label class="form-label" style="margin-bottom:8px;">邀请好友</label>
                    <div style="display:flex;gap:0;border:1px solid var(--border-subtle);border-radius:var(--radius-lg);overflow:hidden;height:240px;">
                        <!-- 左侧：已选 -->
                        <div class="invite-left-panel" style="border-right:1px solid var(--border-subtle);">
                            <div class="invite-panel-header">
                                <span>已选好友</span>
                                <span class="invite-selected-count" id="createSelectedCount">0</span>
                            </div>
                            <div id="createSelectedList" class="invite-selected-list">
                                <div class="invite-empty-hint">点击右侧好友添加</div>
                            </div>
                        </div>
                        <!-- 右侧：搜索+列表 -->
                        <div class="invite-right-panel">
                            <div class="invite-panel-header">
                                <input type="text" id="createSearchInput" class="input-control" placeholder="搜索好友..." style="height:32px;font-size:13px;">
                            </div>
                            <div id="friendListContainer" class="invite-friend-list"></div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button class="btn btn-secondary" id="cancelCreateBtn">取消</button>
                <button class="btn btn-primary" id="confirmCreateBtn">创建</button>
            </div>
        </div>`;
        document.body.appendChild(modal);
        lucide.createIcons();

        let selectedFriends = new Map(); // userId -> {nickname, avatar}
        let allFriends = [];

        const loadFriends = async () => {
            const res = await API.get('/api/friend/list');
            if (res && res.code === 200) {
                const groups = res.data || [];
                allFriends = [];
                for (const group of groups) {
                    for (const f of (group.friends || [])) {
                        if (!allFriends.some(x => x.friendId === f.friendId)) {
                            allFriends.push({ ...f, groupName: group.name });
                        }
                    }
                }
                renderFriendList(allFriends);
            }
        };

        const renderFriendList = (friends) => {
            const container = document.getElementById('friendListContainer');
            if (!friends || friends.length === 0) {
                container.innerHTML = '<div class="invite-empty-hint">暂无好友</div>';
                return;
            }
            container.innerHTML = friends.map(f => {
                const isSelected = selectedFriends.has(f.friendId);
                const avatarSrc = Utils.getAvatarUrl(f.avatar, `user-${f.friendId}`);
                return `
                <div class="invite-friend-item ${isSelected ? 'selected' : ''}" data-user-id="${f.friendId}">
                    <div style="position:relative;flex-shrink:0;">
                        <img src="${avatarSrc}" class="avatar avatar-sm">
                    </div>
                    <div style="flex:1;min-width:0;">
                        <div style="font-size:13px;font-weight:500;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${Utils.escapeHtml(f.nickname)}</div>
                        <div style="font-size:11px;color:var(--text-muted);">${Utils.escapeHtml(f.groupName || '')}</div>
                    </div>
                    <div style="flex-shrink:0;">
                        <i data-lucide="${isSelected ? 'check-circle' : 'circle'}" style="width:18px;height:18px;color:${isSelected ? 'var(--color-primary)' : 'var(--slate-300)'};"></i>
                    </div>
                </div>`;
            }).join('');

            container.querySelectorAll('.invite-friend-item').forEach(item => {
                item.addEventListener('click', () => {
                    const userId = parseInt(item.dataset.userId);
                    if (selectedFriends.has(userId)) {
                        selectedFriends.delete(userId);
                    } else {
                        const f = allFriends.find(x => x.friendId === userId);
                        if (f) selectedFriends.set(userId, { nickname: f.nickname, avatar: f.avatar });
                    }
                    renderFriendList(filterFriends());
                    renderSelectedList();
                });
            });
            lucide.createIcons();
        };

        const renderSelectedList = () => {
            const container = document.getElementById('createSelectedList');
            const countEl = document.getElementById('createSelectedCount');
            countEl.textContent = selectedFriends.size;

            if (selectedFriends.size === 0) {
                container.innerHTML = '<div class="invite-empty-hint">点击右侧好友添加</div>';
                return;
            }
            container.innerHTML = Array.from(selectedFriends.entries()).map(([userId, info]) => {
                const avatarSrc = Utils.getAvatarUrl(info.avatar, `user-${userId}`);
                return `
                <div class="invite-selected-chip">
                    <img src="${avatarSrc}" class="avatar" style="width:20px;height:20px;">
                    <span>${Utils.escapeHtml(info.nickname)}</span>
                    <span class="invite-chip-remove" data-user-id="${userId}"><i data-lucide="x" style="width:12px;height:12px;"></i></span>
                </div>`;
            }).join('');

            container.querySelectorAll('.invite-chip-remove').forEach(btn => {
                btn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    const userId = parseInt(btn.dataset.userId);
                    selectedFriends.delete(userId);
                    renderFriendList(filterFriends());
                    renderSelectedList();
                });
            });
            lucide.createIcons();
        };

        const filterFriends = () => {
            const keyword = (document.getElementById('createSearchInput')?.value || '').trim().toLowerCase();
            if (!keyword) return allFriends;
            return allFriends.filter(f =>
                (f.nickname && f.nickname.toLowerCase().includes(keyword)) ||
                (f.username && f.username.toLowerCase().includes(keyword))
            );
        };

        document.getElementById('createSearchInput').addEventListener('input', () => {
            renderFriendList(filterFriends());
        });

        const closeModal = () => modal.remove();
        modal.querySelector('.modal-close').addEventListener('click', closeModal);
        document.getElementById('cancelCreateBtn').addEventListener('click', closeModal);
        modal.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });

        document.getElementById('confirmCreateBtn').addEventListener('click', async () => {
            const name = document.getElementById('groupName').value.trim();
            if (!name) { Utils.showToast('请输入群名称', 'error'); return; }
            const announcement = document.getElementById('groupAnnouncement').value.trim();
            const res = await API.post('/api/group/create', { name, announcement, memberIds: Array.from(selectedFriends.keys()) });
            if (res && res.code === 200) {
                Utils.showToast('创建成功', 'success');
                closeModal();
                await GroupManager.loadMyGroups();
                if (onSuccess) onSuccess(res.data);
                GroupManager.openGroupChat(res.data.id);
            } else { Utils.showToast(res?.message || '创建失败', 'error'); }
        });

        loadFriends();
    }
};