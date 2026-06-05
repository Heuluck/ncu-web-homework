/**
 * AI 机器人管理器
 */
const BotManager = {
    myBots: [],      // 我创建的机器人列表
    groupBots: [],   // 当前群的机器人列表

    async loadMyBots() {
        const res = await API.get('/api/bot/my-bots');
        if (res && res.code === 200) {
            this.myBots = res.data || [];
        }
    },

    async loadGroupBots(groupId) {
        const res = await API.get(`/api/bot/group/${groupId}`);
        if (res && res.code === 200) {
            this.groupBots = res.data || [];
        }
        return this.groupBots;
    },

    async createBot(dto) {
        const res = await API.post('/api/bot/create', dto);
        if (res && res.code === 200) {
            this.myBots.push(res.data);
            return res.data;
        }
        Utils.showToast(res?.message || '创建失败', 'error');
        return null;
    },

    async updateBot(botId, dto) {
        const res = await API.put(`/api/bot/${botId}`, dto);
        if (res && res.code === 200) {
            const idx = this.myBots.findIndex(b => b.id === botId);
            if (idx !== -1) this.myBots[idx] = res.data;
            return res.data;
        }
        Utils.showToast(res?.message || '更新失败', 'error');
        return null;
    },

    async deleteBot(botId) {
        const res = await API.delete(`/api/bot/${botId}`);
        if (res && res.code === 200) {
            this.myBots = this.myBots.filter(b => b.id !== botId);
            return true;
        }
        Utils.showToast(res?.message || '删除失败', 'error');
        return false;
    },

    async addBotToGroup(botId, groupId) {
        const res = await API.post(`/api/bot/${botId}/group/${groupId}`);
        if (res && res.code === 200) {
            Utils.showToast('已添加到群聊', 'success');
            return true;
        }
        Utils.showToast(res?.message || '添加失败', 'error');
        return false;
    },

    async removeBotFromGroup(botId, groupId) {
        const res = await API.delete(`/api/bot/${botId}/group/${groupId}`);
        if (res && res.code === 200) {
            this.groupBots = this.groupBots.filter(b => b.id !== botId);
            Utils.showToast('已从群聊移除', 'success');
            return true;
        }
        Utils.showToast(res?.message || '移除失败', 'error');
        return false;
    },

    /** 获取触发条件描述 */
    getTriggerLabel(triggerType) {
        switch (triggerType) {
            case 0: return '@触发';
            case 1: return '每次触发';
            case 2: return '随机概率';
            default: return '未知';
        }
    },

    /** 获取默认头像 */
    getDefaultBotAvatar(name) {
        const seed = name || 'bot';
        return `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(seed)}`;
    }
};
