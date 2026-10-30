import { createRouter, createWebHistory } from 'vue-router'

// 路由表：按 SRS 第 3 章功能模块划分，页面骨架逐步填充
const routes = [
  { path: '/login', name: 'login', component: () => import('../views/login/index.vue') },
  {
    path: '/',
    component: () => import('../layout/index.vue'),
    redirect: '/dashboard',
    children: [
      // roles 为空表示三类角色均可访问；数据范围由后端行级隔离控制
      { path: 'dashboard', name: 'dashboard', meta: { title: '统计看板', icon: 'Odometer', roles: ['ADMIN', 'LEADER'] }, component: () => import('../views/dashboard/index.vue') },
      { path: 'archive', name: 'archive', meta: { title: '档案管理', icon: 'FolderOpened' }, component: () => import('../views/archive/index.vue') },
      { path: 'archive/fill', name: 'archiveFill', meta: { title: '填报档案', roles: ['ASSOC'] }, component: () => import('../views/archive/Fill.vue') },
      { path: 'archive/detail', name: 'archiveDetail', meta: { title: '档案详情' }, component: () => import('../views/archive/Detail.vue') },
      { path: 'review', name: 'review', meta: { title: '审核归档', icon: 'Checked', roles: ['ADMIN'] }, component: () => import('../views/review/index.vue') },
      { path: 'borrow', name: 'borrow', meta: { title: '档案借阅', icon: 'Reading' }, component: () => import('../views/borrow/index.vue') },
      { path: 'task', name: 'task', meta: { title: '专项补录', icon: 'AlarmClock' }, component: () => import('../views/task/index.vue') },
      { path: 'task/detail', name: 'taskDetail', meta: { title: '专项补录详情' }, component: () => import('../views/task/Detail.vue') },
      { path: 'push', name: 'push', meta: { title: '政策推送', icon: 'Promotion' }, component: () => import('../views/push/index.vue') },
      { path: 'push/detail', name: 'pushDetail', meta: { title: '政策推送详情' }, component: () => import('../views/push/Detail.vue') },
      { path: 'association', name: 'association', meta: { title: '商协会名录', icon: 'OfficeBuilding', roles: ['ADMIN', 'LEADER'] }, component: () => import('../views/association/index.vue') },
      { path: 'association/create', name: 'associationCreate', meta: { title: '新增商协会', roles: ['ADMIN'] }, component: () => import('../views/association/Form.vue') },
      { path: 'association/edit', name: 'associationEdit', meta: { title: '修改商协会', roles: ['ADMIN'] }, component: () => import('../views/association/Form.vue') },
      { path: 'association/detail', name: 'associationDetail', meta: { title: '商协会详情', roles: ['ADMIN', 'LEADER'] }, component: () => import('../views/association/Detail.vue') },
      { path: 'audit', name: 'audit', meta: { title: '审计日志', icon: 'Document', roles: ['ADMIN', 'LEADER'] }, component: () => import('../views/audit/index.vue') },
      { path: 'account', name: 'account', meta: { title: '账号管理', icon: 'User' }, component: () => import('../views/account/index.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({ history: createWebHistory(), routes })

// 登录守卫：无 token 一律回登录页；带 roles 的页面按角色拦截（协会管理员进档案管理）
router.beforeEach((to) => {
  if (to.path !== '/login' && !localStorage.getItem('token')) return '/login'
  const role = localStorage.getItem('roleCode')
  if (to.meta?.roles?.length && !to.meta.roles.includes(role)) return '/archive'
})

export default router
