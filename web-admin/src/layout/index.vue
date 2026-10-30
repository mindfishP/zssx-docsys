<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">商协会档案系统</div>
      <el-menu :default-active="$route.path" router background-color="#1a2330"
               text-color="#b0b8c0" active-text-color="#d4875a">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div />
        <el-dropdown @command="onCommand">
          <span class="user">
            {{ user.realName }}（{{ roleLabel }}）
            <el-tag v-if="user.assocCode" size="small" type="warning">{{ user.assocCode }}</el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main><router-view :key="$route.fullPath" /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const router = useRouter()
const user = useUserStore()

const allMenus = [
  // 统计看板与商协会名录面向主管部门与领导，协会管理员不需要
  { path: '/dashboard', title: '统计看板', icon: 'Odometer', staff: true },
  { path: '/archive', title: '档案管理', icon: 'FolderOpened' },
  { path: '/review', title: '审核归档', icon: 'Checked', admin: true },
  { path: '/borrow', title: '档案借阅', icon: 'Reading' },
  { path: '/task', title: '专项补录', icon: 'AlarmClock' },
  { path: '/push', title: '政策推送', icon: 'Promotion' },
  { path: '/association', title: '商协会名录', icon: 'OfficeBuilding', staff: true },
  { path: '/audit', title: '审计日志', icon: 'Document', staff: true },
  { path: '/account', title: '账号管理', icon: 'User' }
]
// 菜单按角色过滤（SRS 表 4-1 权限矩阵）
const menus = allMenus.filter((m) => {
  if (m.admin) return user.isAdmin                     // 审核归档：仅主管部门操作员
  if (m.staff) return user.isAdmin || user.isLeader    // 看板/名录/日志：主管部门与领导
  return true                                          // 其余页面三类角色均可见（数据范围后端控制）
})
const roleLabel = computed(
  () => ({ ADMIN: '主管部门操作员', LEADER: '主管领导', ASSOC: '商协会管理员' })[user.roleCode] || user.roleCode
)

function onCommand(cmd) {
  if (cmd === 'logout') {
    user.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside {
  background: #1a2330;
  position: relative;
  overflow: hidden;
}
/* 侧边栏最左侧的细橙线：贯穿整条导航 */
.aside::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: #d4875a;
  z-index: 2;
}
.logo { color: #fff; font-weight: 700; text-align: center; padding: 18px 0; letter-spacing: 2px; }
.header { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #eee; }
.user { cursor: pointer; color: #333; }

/* 每个菜单项自带一段橙线，与侧边栏基线重叠；悬停/选中时该段加宽 */
:deep(.el-menu-item) {
  position: relative;
}
:deep(.el-menu-item)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: #d4875a;
  transition: width 0.2s ease, box-shadow 0.2s ease;
  z-index: 3;
}
:deep(.el-menu-item:hover)::before,
:deep(.el-menu-item.is-active)::before {
  width: 8px;
  box-shadow: 2px 0 10px rgba(212, 135, 90, 0.45);
}
</style>
