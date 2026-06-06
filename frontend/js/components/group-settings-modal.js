const GroupSettingsModal = {
    async show(groupId) {
        const infoRes = await API.get(`/api/group/info/${groupId}`);
        const groupInfo = (infoRes && infoRes.code === 200) ? infoRes.data : {};
        const isOwner = groupInfo.myRole === 2;
        const canEdit = groupInfo.myRole >= 1; // 群主或管理员可编辑
        const readonlyAttr = canEdit ? '' : 'readonly';
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.innerHTML = `<div class="modal-container" style="max-width:450px;"><div class="modal-header"><h3>群设置</h3><button class="modal-close btn-icon"><i data-lucide="x"></i></button></div>
      <div class="modal-body"><div class="settings-section"><div class="section-title">群名称</div><input type="text" id="settingsGroupName" class="input-control" value="${Utils.escapeHtml(groupInfo.name || '')}" ${readonlyAttr}></div>
      <div class="settings-section"><div class="section-title">群公告</div><textarea id="settingsAnnouncement" class="input-control" rows="3" style="resize:vertical;" ${readonlyAttr}>${Utils.escapeHtml(groupInfo.announcement || '')}</textarea></div>
      ${!canEdit ? '<div style="font-size:12px;color:var(--text-muted);margin-top:8px;">仅群主和管理员可修改群信息</div>' : ''}
      </div>
      <div class="modal-footer" style="flex-direction:column;gap:8px;">
        ${canEdit ? `<div style="display:flex;gap:8px;width:100%;">
          <button class="btn btn-secondary" id="closeSettingsBtn" style="flex:1;">关闭</button>
          <button class="btn btn-primary" id="saveSettingsBtn" style="flex:1;">保存</button>
        </div>` : `<button class="btn btn-secondary" id="closeSettingsBtn" style="width:100%;">关闭</button>`}
        ${isOwner ? '<button class="btn btn-danger" id="disbandGroupBtn" style="width:100%;">解散群聊</button>' : ''}
      </div></div>`;
        document.body.appendChild(modal);
        lucide.createIcons();

        // 遮罩点击关闭
        modal.addEventListener('click', (e) => { if (e.target === modal) modal.remove(); });

        const closeModal = () => modal.remove();
        modal.querySelector('.modal-close').addEventListener('click', closeModal);
        document.getElementById('closeSettingsBtn').addEventListener('click', closeModal);

        document.getElementById('saveSettingsBtn').addEventListener('click', async () => {
            const name = document.getElementById('settingsGroupName').value.trim();
            if (!name) { Utils.showToast('群名称不能为空', 'error'); return; }
            const announcement = document.getElementById('settingsAnnouncement').value;
            const res = await API.put(`/api/group/info/${groupId}`, { name, announcement });
            if (res && res.code === 200) {
                Utils.showToast('保存成功', 'success');
                closeModal();
                await GroupManager.loadMyGroups();
                if (GroupManager.currentGroupId === groupId) {
                    await GroupManager.openGroupChat(groupId);
                }
            } else {
                Utils.showToast(res?.message || '保存失败', 'error');
            }
        });

        const disbandBtn = document.getElementById('disbandGroupBtn');
        if (disbandBtn) {
            disbandBtn.addEventListener('click', async () => {
                if (!await Utils.showConfirm('确定解散群聊？不可恢复！', { title: '解散群聊', confirmText: '解散', danger: true })) return;
                const res = await API.delete(`/api/group/disband/${groupId}`);
                if (res && res.code === 200) {
                    Utils.showToast('群聊已解散', 'success');
                    closeModal();
                    if (GroupManager.currentGroupId === groupId) {
                        GroupManager.currentGroupId = null;
                        GroupManager.currentGroupInfo = null;
                        document.getElementById('chatHeader').style.display = 'none';
                        document.getElementById('chatInputArea').style.display = 'none';
                        document.getElementById('chatMessages').innerHTML = `
                            <div class="empty-state">
                                <div class="empty-state-icon"><i data-lucide="message-square"></i></div>
                                <div class="empty-state-title">群聊已解散</div>
                                <div class="empty-state-text">该群聊已被解散</div>
                            </div>`;
                        lucide.createIcons();
                    }
                    await GroupManager.loadMyGroups();
                } else {
                    Utils.showToast(res?.message || '解散失败', 'error');
                }
            });
        }
    }
};