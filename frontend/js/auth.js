/**
 * 认证管理
 * Token 存取、登录状态检查
 */
const Auth = {
  TOKEN_KEY: 'chat_token',
  USER_KEY: 'chat_user',

  isLoggedIn() {
    return !!this.getToken();
  },

  getToken() {
    return localStorage.getItem(this.TOKEN_KEY);
  },

  setToken(token) {
    localStorage.setItem(this.TOKEN_KEY, token);
  },

  getUser() {
    const user = localStorage.getItem(this.USER_KEY);
    return user ? JSON.parse(user) : null;
  },

  setUser(user) {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  },

  logout() {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    window.location.href = '/login.html';
  },

  checkAuth() {
    if (!this.isLoggedIn()) {
      window.location.href = '/login.html';
      return false;
    }
    return true;
  },

  // 获取当前用户 ID
  getUserId() {
    const user = this.getUser();
    return user ? user.id : null;
  },

  // 获取当前用户昵称
  getUsername() {
    const user = this.getUser();
    return user ? (user.nickname || user.username) : '';
  }
};
