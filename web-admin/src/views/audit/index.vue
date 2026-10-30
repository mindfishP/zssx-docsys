<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="q.action" placeholder="操作动作关键词" clearable style="width: 220px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="logId" label="编号" width="80" />
      <el-table-column prop="operateAt" label="操作时间" width="170" />
      <el-table-column prop="operateBy" label="操作人" width="100" />
      <el-table-column prop="action" label="动作" width="130" />
      <el-table-column prop="paramsBrief" label="参数摘要" min-width="220" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="result" label="结果" min-width="140" show-overflow-tooltip />
      <el-table-column prop="costMs" label="耗时(ms)" width="90" />
    </el-table>

    <el-pagination style="margin-top: 12px" layout="total, prev, pager, next"
      :total="total" :page-size="q.size" v-model:current-page="q.page" @current-change="load" />
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageAuditLog } from '../../api'

const rows = ref([])
const total = ref(0)
const q = reactive({ page: 1, size: 20, action: '' })

async function load() {
  const data = await pageAuditLog(q)
  rows.value = data.records
  total.value = Number(data.total)
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
</style>
