const CreateGroupModal = {
    show(onSuccess) {
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.innerHTML = `<div class="modal-container" style="max-width:450px;"><div class="modal-header"><h3>创建群聊</h3><button class="modal-close btn-icon"><i data-lucide="x"></i></button></div>
      <div class="modal-body"><div class="form-group"><label class="form-label">群名称</label><input type="text" id="groupName" class="input-control" placeholder="输入群名称"></div>
      <div class="form-group"><label class="form-label">群公告</label><textarea id="groupAnnouncement" class="input-control" rows="2" placeholder="群公告（可选）"></textarea></div>
      <div class="form-group"><label class="form-label">邀请好友</label><div id="friendListContainer" class="friend-list-container" style="max-height:200px;overflow-y:auto;"></div></div></div>
      <div class="modal-footer"><button class="btn btn-secondary" id="cancelCreateBtn">取消</button><button class="btn btn-primary" id="confirmCreateBtn">创建</button></div></div>`;
        document.body.appendChild(modal);
        lucide.createIcons();

        let selectedFriends = new Set();
        let friendsList = [];

        const loadFriends = async () => {
            const res = await API.get('/api/friend/list');
            if (res && res.code === 200) {
                friendsList = res.data || [];
                renderFriendList();
            }
        };
        const renderFriendList = () => {
            const container = document.getElementById('friendListContainer');
            if (friendsList.length === 0) { container.innerHTML = '<div class="empty-state-text">暂无好友</div>'; return; }
            container.innerHTML = friendsList.map(f => `<div class="friend-item ${selectedFriends.has(f.userId) ? 'selected' : ''}" data-user-id="${f.userId}">
        <img src="${Utils.getAvatarUrl(f.avatar, `user-${f.userId}`)}" class="avatar avatar-sm"><span class="friend-name">${Utils.escapeHtml(f.nickname)}</span>
        <div class="friend-checkbox"><i data-lucide="${selectedFriends.has(f.userId) ? 'check-circle' : 'circle'}"></i></div></div>`).join('');
            container.querySelectorAll('.friend-item').forEach(item => {
                item.addEventListener('click', () => {
                    const userId = parseInt(item.dataset.userId);
                    selectedFriends.has(userId) ? selectedFriends.delete(userId) : selectedFriends.add(userId);
                    renderFriendList();
                });
            });
            lucide.createIcons();
        };

        const closeModal = () => modal.remove();
        modal.querySelector('.modal-close').addEventListener('click', closeModal);
        document.getElementById('cancelCreateBtn').addEventListener('click', closeModal);
        modal.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });

        document.getElementById('confirmCreateBtn').addEventListener('click', async () => {
            const name = document.getElementById('groupName').value.trim();
            if (!name) { Utils.showToast('请输入群名称', 'error'); return; }
            const announcement = document.getElementById('groupAnnouncement').value.trim();
            const res = await API.post('/api/group/create', { name, announcement, memberIds: Array.from(selectedFriends) });
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