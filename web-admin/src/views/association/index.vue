<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="q.industryType" placeholder="行业类别" clearable style="width: 140px">
        <el-option v-for="i in industries" :key="i" :label="i" :value="i" />
      </el-select>
      <el-input v-model="q.keyword" placeholder="协会名称" clearable style="width: 200px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="user.isAdmin" type="success" @click="goCreate">新增协会</el-button>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="assocCode" label="编号" width="90" />
      <el-table-column prop="assocName" label="协会名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="industryType" label="行业" width="90" />
      <el-table-column prop="legalPerson" label="法定代表人" width="100" />
      <el-table-column prop="contactPhone" label="联系电话" width="140" show-overflow-tooltip />
      <el-table-column prop="registerDate" label="登记日期" width="120" />
      <el-table-column label="活跃度分档" width="110">
        <template #default="{ row }">
          <el-tag :type="{ 1: 'success', 2: 'warning', 3: 'info' }[row.activityLevel]">
            {{ { 1: '示范型', 2: '成长型', 3: '休眠型' }[row.activityLevel] || '未评估' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="activityScore" label="活跃度得分" width="100" />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          <el-button v-if="user.isAdmin" link type="primary" @click="goEdit(row)">修改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top: 12px" layout="total, prev, pager, next"
      :total="total" :page-size="q.size" v-model:current-page="q.page" @current-change="load" />
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageAssociation } from '../../api'
import { useUserStore } from '../../store/user'

const router = useRouter()
const user = useUserStore()
const industries = ['灯饰', '五金', '家电', '服装', '家具', '其他']

const rows = ref([])
const total = ref(0)
const q = reactive({ page: 1, size: 20, industryType: '', keyword: '' })

async function load() {
  const data = await pageAssociation(q)
  rows.value = data.records
  total.value = Number(data.total)
}

function goCreate() {
  router.push('/association/create')
}

function goEdit(row) {
  router.push({ path: '/association/edit', query: { code: row.assocCode } })
}

// 查看改为独立详情页，页面内同时展示该协会的成员账号
function openDetail(row) {
  router.push({ path: '/association/detail', query: { code: row.assocCode } })
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
</style>
