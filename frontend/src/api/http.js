import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 统一 axios 实例：后端返回 { code, msg, data }
 * 成功直接返回 data；失败弹提示并 reject { status, msg }
 */
const http = axios.create({
  baseURL: '/',
  timeout: 30000,
  withCredentials: true
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    // 文件流（Excel 导出/二维码等）直接透传
    if (res.config.responseType === 'blob') return res
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      ElMessage.error(body.msg || '操作失败')
      return Promise.reject({ status: body.code, msg: body.msg })
    }
    return body
  },
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.msg || err.message || '网络异常'
    if (status === 401) {
      // 会话失效：回到登录页（消费者公开页不经过此拦截）
      if (!location.pathname.startsWith('/app/login')) {
        location.href = '/app/login'
      }
      return Promise.reject({ status, msg })
    }
    ElMessage.error(msg)
    return Promise.reject({ status, msg })
  }
)

export default http
