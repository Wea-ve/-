/** 通用格式化与字典翻译 */

export function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

export function fmtDate(d) {
  if (!d) return '-'
  return String(d).slice(0, 10)
}

export const batchStatusMap = {
  PLANTING: { text: '种植中', type: 'warning' },
  HARVESTED: { text: '已采收', type: 'primary' },
  SOLD: { text: '已售罄', type: 'info' }
}

export const traceTypeMap = {
  HARVEST: { text: '采收', color: '#43a047' },
  INBOUND: { text: '入库', color: '#1e88e5' },
  OUTBOUND: { text: '出库', color: '#fb8c00' },
  SALE: { text: '销售', color: '#8e24aa' }
}

export const logTypeMap = {
  SOWING: { text: '播种', color: '#43a047' },
  FERTILIZE: { text: '施肥', color: '#6d4c41' },
  PESTICIDE: { text: '打药', color: '#e53935' },
  IRRIGATE: { text: '灌溉', color: '#1e88e5' },
  WEED: { text: '除草', color: '#fdd835' }
}

export const complaintStatusMap = {
  PENDING: { text: '待处理', type: 'danger' },
  PROCESSING: { text: '处理中', type: 'warning' },
  CLOSED: { text: '已关闭', type: 'info' }
}

export const complaintTypeMap = {
  QUALITY: '质量问题',
  WEIGHT: '缺斤少两',
  FAKE: '信息不实',
  OTHER: '其他'
}

/** 触发浏览器下载文件流 */
export function downloadBlob(res, filename) {
  const url = window.URL.createObjectURL(new Blob([res.data]))
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  window.URL.revokeObjectURL(url)
}
