/**
 * API 请求封装
 * 统一处理 Token、错误、响应格式
 */
const API = {
  baseURL: '',

  async request(url, options = {}) {
    const token = Auth.getToken();
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers
    };
    
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    try {
      const response = await fetch(this.baseURL + url, {
        ...options,
        headers
      });
      
      const data = await response.json();

      // Token 过期或无效，跳转登录
      if (data.code === 401) {
        Auth.logout();
        return null;
      }

      return data;
    } catch (error) {
      console.error('API Error:', error);
      return { code: 500, message: '网络错误，请稍后重试' };
    }
  },

  get(url) {
    return this.request(url, { method: 'GET' });
  },

  post(url, body) {
    return this.request(url, {
      method: 'POST',
      body: JSON.stringify(body)
    });
  },

  put(url, body) {
    return this.request(url, {
      method: 'PUT',
      body: JSON.stringify(body)
    });
  },

  delete(url) {
    return this.request(url, { method: 'DELETE' });
  },

  async upload(file) {
    const token = Auth.getToken();
    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch('/api/file/upload', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        body: formData
      });
      return response.json();
    } catch (error) {
      console.error('Upload Error:', error);
      return { code: 500, message: '文件上传失败' };
    }
  }
};
