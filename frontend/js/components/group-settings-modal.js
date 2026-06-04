const GroupSettingsModal = {
    async show(groupId) {
        const infoRes = await API.get(`/api/group/info/${groupId}`);
        const groupInfo = (infoRes && infoRes.code === 200) ? infoRes.data : {};
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.innerHTML = `<div class="modal-container" style="max-width:450px;"><div class="modal-header"><h3>群设置</h3><button class="modal-close btn-icon"><i data-lucide="x"></i></button></div>
      <div class="modal-body"><div class="settings-section"><div class="section-title">群名称</div><input type="text" id="settingsGroupName" class="input-control" value="${Utils.escapeHtml(groupInfo.name || '')}"></div>
      <div class="settings-section"><div class="section-title">群公告</div><textarea id="settingsAnnouncement" class="input-control" rows="2">${Utils.escapeHtml(groupInfo.announcement || '')}</textarea></div>
      ${groupInfo.myRole === 2 ? `<div class="settings-section danger-zone"><button class="btn btn-danger" id="disbandGroupBtn">解散群聊</button></div>` : ''}</div>
      <div class="modal-footer"><button class="btn btn-secondary" id="closeSettingsBtn">关闭</button><button class="btn btn-primary" id="saveSettingsBtn">保存</button></div></div>`;
        document.body.appendChild(modal);
        lucide.createIcons();

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
                if (!confirm('确定解散群聊？不可恢复！')) return;
                const res = await API.delete(`/api/group/disband/${groupId}`);
                if (res && res.code === 200) {
                    Utils.showToast('群聊已解散', 'success');
                    closeModal();

                    // 清空当前群聊界面
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

                    // 刷新群列表
                    await GroupManager.loadMyGroups();
                } else {
                    Utils.showToast(res?.message || '解散失败', 'error');
                }
            });
        }
    }
};