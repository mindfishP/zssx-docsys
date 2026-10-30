import { defineStore } from 'pinia'

/**
 * 登录用户状态（SRS 2.1 三类角色）。
 * roleCode: ADMIN 主管部门操作员 / LEADER 主管领导只读 / ASSOC 商协会管理员
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    realName: localStorage.getItem('realName') || '',
    roleCode: localStorage.getItem('roleCode') || '',
    assocCode: localStorage.getItem('assocCode') || ''
  }),
  getters: {
    isAdmin: (s) => s.roleCode === 'ADMIN',
    isLeader: (s) => s.roleCode === 'LEADER',
    isAssoc: (s) => s.roleCode === 'ASSOC',
    // 主管领导全程只读（SRS 4.2 规则 4）
    readonly: (s) => s.roleCode === 'LEADER'
  },
  actions: {
    save(info) {
      this.token = info.token
      this.username = info.username
      this.realName = info.realName || info.username
      this.roleCode = info.roleCode
      this.assocCode = info.assocCode || ''
      localStorage.setItem('token', info.token)
      localStorage.setItem('username', info.username)
      localStorage.setItem('realName', this.realName)
      localStorage.setItem('roleCode', info.roleCode)
      localStorage.setItem('assocCode', this.assocCode)
    },
    /** 修改姓名后同步本地显示（账号与姓名分开维护） */
    setRealName(name) {
      this.realName = name || this.username
      localStorage.setItem('realName', this.realName)
    },
    logout() {
      localStorage.clear()
      this.$reset()
    }
  }
})
