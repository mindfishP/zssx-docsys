// 展示层格式化：时间、状态、文件大小
const ARCHIVE_STATUS = {
  0: { text: '草稿', cls: 'tag-info' },
  1: { text: '待审核', cls: 'tag-warn' },
  2: { text: '审核退回', cls: 'tag-danger' },
  3: { text: '已归档', cls: 'tag-success' }
}

const BORROW_STATUS = {
  0: { text: '待审批', cls: 'tag-warn' },
  1: { text: '借阅中', cls: 'tag-primary' },
  2: { text: '已归还', cls: 'tag-success' },
  4: { text: '已拒绝', cls: 'tag-info' }
}

function archiveStatus(status) {
  return ARCHIVE_STATUS[status] || { text: '未知', cls: 'tag-info' }
}

function borrowStatus(status) {
  return BORROW_STATUS[status] || { text: '未知', cls: 'tag-info' }
}

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

function fileSize(bytes) {
  if (bytes === null || bytes === undefined) return '—'
  if (bytes > 1048576) return (bytes / 1048576).toFixed(1) + ' MB'
  return Math.max(1, Math.round(bytes / 1024)) + ' KB'
}

function levelText(level) {
  return { 1: '示范型', 2: '成长型', 3: '休眠型' }[level] || '未评估'
}

module.exports = { archiveStatus, borrowStatus, formatTime, fileSize, levelText }
