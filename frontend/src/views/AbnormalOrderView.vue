<template>
  <div class="page abnormal-page">
    <!-- ===== 处置单列表 ===== -->
    <section id="sec-orders" class="wp-section">
      <div class="section-head">
        <h3 class="wp-section-title">异常工况处置单 · 炉况 / 告警 / 抓斗取证与投料联锁闭环</h3>
        <div class="head-actions">
          <el-radio-group v-model="statusFilter" size="small" @change="loadList">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button v-for="s in dict.statuses || []" :key="s.code" :label="s.code">{{ s.name }}</el-radio-button>
          </el-radio-group>
          <el-button size="small" @click="loadAll">刷新</el-button>
          <el-button size="small" type="warning" plain :loading="loading" @click="scanSevere">同步存量严重告警</el-button>
          <el-button size="small" type="primary" @click="openManual">人工发起</el-button>
        </div>
      </div>

      <div class="wp-panel">
        <div class="wp-panel-body">
          <table class="wp-table">
            <thead>
              <tr>
                <th>处置单</th>
                <th>炉号</th>
                <th>状态</th>
                <th>触发来源</th>
                <th>触发原因</th>
                <th>投料权限</th>
                <th>负责人</th>
                <th>复核人</th>
                <th>版本</th>
                <th>更新时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in orders" :key="o.id" :class="{ active: selected && selected.id === o.id }" @click="selectOrder(o)">
                <td class="mono strong">{{ o.orderNo }}</td>
                <td class="mono strong">{{ o.furnaceCode }}</td>
                <td><span class="wp-tag" :class="statusTag(o.status)">{{ o.statusName }}</span></td>
                <td>{{ sourceName(o.triggerSource) }}</td>
                <td class="reason-cell">{{ o.triggerReason }}</td>
                <td>
                  <span class="wp-tag" :class="o.feedingLocked ? 'danger' : 'ok'">
                    {{ o.feedingLocked ? '冻结' : '允许' }}
                  </span>
                </td>
                <td>{{ o.ownerName || '--' }}</td>
                <td>{{ o.reviewerName || '--' }}</td>
                <td class="mono">v{{ o.version }}</td>
                <td class="mono">{{ time(o.updatedAt) }}</td>
              </tr>
              <tr v-if="!orders.length"><td colspan="10" class="empty">暂无处置单</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <template v-if="selected">
      <!-- ===== 闭环状态与操作 ===== -->
      <section class="wp-section">
        <h3 class="wp-section-title">处置闭环 · {{ selected.orderNo }}</h3>
        <el-steps :active="stepIndex(selected.status)" align-center class="order-steps">
          <el-step title="待处置" />
          <el-step title="处置中" />
          <el-step title="待复核" />
          <el-step title="已恢复" />
          <el-step title="已关闭" />
        </el-steps>
        <el-alert
          class="lock-alert"
          :type="selected.feedingLocked ? 'error' : 'success'"
          :closable="false"
          show-icon
          :title="selected.feedingLocked ? `后续投料已冻结：${selected.freezeReason || '异常工况处置中'}` : '投料联锁已解除，抓斗可恢复供料'"
        />

        <div class="action-grid">
          <div class="wp-panel action-card">
            <div class="wp-panel-title"><span>负责人处置</span><span class="sub">操作人必须是负责人</span></div>
            <div class="wp-panel-body">
              <el-form label-width="86px" size="small">
                <el-form-item label="负责人">
                  <el-input v-model="ownerName" placeholder="负责人姓名" />
                </el-form-item>
                <el-form-item label="复核人">
                  <el-input v-model="reviewerName" placeholder="另一名复核人员" />
                </el-form-item>
                <el-form-item label="处置措施">
                  <el-input v-model="measures" type="textarea" :rows="3" placeholder="记录燃烧调整、停料、巡检或排放处理措施" />
                </el-form-item>
                <el-form-item label="申请说明">
                  <el-input v-model="reviewNote" type="textarea" :rows="2" placeholder="申请复核时填写" />
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" plain :disabled="!canStart" :loading="loading" @click="doStart">开始处置</el-button>
                  <el-button type="primary" plain :disabled="!canMeasure" :loading="loading" @click="doSaveMeasures">保存措施</el-button>
                  <el-button type="warning" :disabled="!canRequestReview" :loading="loading" @click="doRequestReview">申请复核</el-button>
                </div>
              </el-form>
            </div>
          </div>

          <div class="wp-panel action-card">
            <div class="wp-panel-title"><span>另一复核人确认</span><span class="sub">不能由负责人本人复核</span></div>
            <div class="wp-panel-body">
              <div v-if="readiness" class="readiness-box">
                <div class="ready-row">
                  <span>燃烧重新达标</span>
                  <b :class="readiness.combustionQualified ? 'ok-text' : 'bad-text'">
                    {{ readiness.combustionQualified ? '是' : '否' }}
                  </b>
                </div>
                <div class="ready-row">
                  <span>严重告警解除</span>
                  <b :class="readiness.allAttachedAlarmsCleared ? 'ok-text' : 'bad-text'">
                    {{ readiness.allAttachedAlarmsCleared ? '是' : '否' }}
                  </b>
                </div>
                <div class="ready-row">
                  <span>告警数据可用</span>
                  <b :class="readiness.dataMissing ? 'bad-text' : 'ok-text'">
                    {{ readiness.dataMissing ? '暂不可用' : '可用' }}
                  </b>
                </div>
                <div class="impact">{{ readiness.impact }}</div>
                <ul v-if="readiness.blockingReasons && readiness.blockingReasons.length" class="block-list">
                  <li v-for="r in readiness.blockingReasons" :key="r">{{ r }}</li>
                </ul>
              </div>
              <el-form label-width="86px" size="small">
                <el-form-item label="复核意见">
                  <el-input v-model="reviewerNote" type="textarea" :rows="2" placeholder="复核恢复意见" />
                </el-form-item>
                <el-form-item v-if="readiness && readiness.dataMissing" label="缺失理由">
                  <el-input v-model="missingReason" type="textarea" :rows="2" placeholder="选择关闭但保持冻结时必须记录理由" />
                </el-form-item>
                <div class="button-row wrap">
                  <el-button :loading="loading" @click="loadReadiness">重新检查恢复条件</el-button>
                  <el-button type="success" :disabled="!canRecover" :loading="loading" @click="doReview('RECOVER')">确认恢复</el-button>
                  <el-button type="warning" plain :disabled="!canReview" :loading="loading" @click="doReview('WAIT')">继续冻结等待补齐</el-button>
                  <el-button type="danger" plain :disabled="!canReview" :loading="loading" @click="doReview('CLOSE_KEEP_FROZEN')">关闭但保持冻结</el-button>
                  <el-button type="primary" plain :disabled="selected.status !== 'RECOVERED'" :loading="loading" @click="doClose">恢复后关闭</el-button>
                </div>
              </el-form>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== 证据快照 ===== -->
      <section id="sec-evidence" class="wp-section">
        <h3 class="wp-section-title">证据快照 · 后续数据刷新不会改写触发时证据</h3>
        <div class="evidence-grid">
          <div class="wp-panel">
            <div class="wp-panel-title"><span>炉况快照</span></div>
            <div class="wp-panel-body">
              <template v-if="selected.furnaceSnapshot">
                <div class="kv"><span>炉膛温度</span><b class="num">{{ num(selected.furnaceSnapshot.furnaceTempC, 1) }} ℃</b></div>
                <div class="kv"><span>停留时间</span><b class="num">{{ num(selected.furnaceSnapshot.flueGasResidenceS, 2) }} s</b></div>
                <div class="kv"><span>CO</span><b class="num">{{ num(selected.furnaceSnapshot.coPpm, 0) }} ppm</b></div>
                <div class="kv"><span>供料抓斗</span><b>{{ selected.furnaceSnapshot.grabNo || '--' }}</b></div>
                <div class="kv"><span>快照时间</span><b>{{ time(selected.furnaceSnapshot.snapshotTime) }}</b></div>
                <el-alert
                  v-for="item in selected.furnaceSnapshot.unqualifiedItems || []"
                  :key="item"
                  :title="item"
                  type="error"
                  :closable="false"
                  class="snapshot-alert"
                />
              </template>
              <div v-else class="empty">无炉况快照</div>
            </div>
          </div>

          <div class="wp-panel">
            <div class="wp-panel-title"><span>同炉严重排放告警（不沿用 handled 标记）</span></div>
            <div class="wp-panel-body">
              <table class="wp-table compact">
                <thead>
                  <tr><th>告警</th><th>指标</th><th>触发值</th><th>最新值</th><th>复核</th><th>来源</th><th>数据状态</th></tr>
                </thead>
                <tbody>
                  <tr v-for="a in selected.alarms || []" :key="a.alarmId">
                    <td class="mono">{{ a.alarmNo }}</td>
                    <td>{{ a.indicatorName }}</td>
                    <td class="num">{{ num(a.measuredValue, 3) }}</td>
                    <td class="num" :class="a.cleared ? 'ok-text' : 'bad-text'">
                      {{ a.latestValue === null || a.latestValue === undefined ? '无数据' : num(a.latestValue, 3) }}
                    </td>
                    <td>
                      <span v-if="a.dataAvailable === false" class="wp-tag warn">数据缺失</span>
                      <span v-else class="wp-tag" :class="a.cleared ? 'ok' : 'danger'">{{ a.cleared ? '已解除' : '未解除' }}</span>
                    </td>
                    <td>{{ attachedName(a.attachedSource) }}</td>
                    <td>
                      <el-button
                        size="small"
                        :type="a.forceMissingData ? 'success' : 'warning'"
                        link
                        @click="toggleMissing(a)"
                      >{{ a.forceMissingData ? '取消缺失' : '标记暂不可用' }}</el-button>
                    </td>
                  </tr>
                  <tr v-if="!(selected.alarms || []).length"><td colspan="7" class="empty">无关联告警</td></tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="wp-panel">
            <div class="wp-panel-title"><span>供料抓斗 / 储坑快照</span></div>
            <div class="wp-panel-body">
              <div v-for="g in selected.grabSnapshots || []" :key="`${g.grabNo}-${g.pitNo}`" class="grab-row">
                <b class="mono">{{ g.grabNo || '--' }}</b>
                <span>{{ g.pitNo || '未匹配坑位' }} {{ g.pitName || '' }}</span>
                <span class="num">库存 {{ num(g.currentStockTon, 2) }} t</span>
                <span>{{ time(g.snapshotTime) }}</span>
              </div>
              <div v-if="!(selected.grabSnapshots || []).length" class="empty">无抓斗快照</div>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== 操作轨迹 ===== -->
      <section id="sec-trace" class="wp-section">
        <h3 class="wp-section-title">复核恢复与失败操作轨迹</h3>
        <div class="wp-panel">
          <div class="wp-panel-body">
            <el-timeline>
              <el-timeline-item
                v-for="e in selected.events || []"
                :key="e.id"
                :type="e.success ? 'success' : 'danger'"
                :timestamp="time(e.createdAt)"
              >
                <div class="event-title">
                  <b>{{ eventName(e.eventType) }}</b>
                  <span class="mono dim">{{ e.operator || '系统' }}</span>
                  <span class="dim">{{ e.fromStatus || '--' }} → {{ e.toStatus || '--' }}</span>
                </div>
                <div v-if="e.detail">{{ e.detail }}</div>
                <div v-if="e.failureReason" class="fail-reason">失败原因：{{ e.failureReason }}</div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </section>
    </template>

    <!-- ===== 人工发起 ===== -->
    <el-dialog v-model="manualDialog" title="人工发起异常工况处置单" width="520px">
      <el-form :model="manual" label-width="92px">
        <el-form-item label="炉号">
          <el-select v-model="manual.furnaceCode" placeholder="选择焚烧炉">
            <el-option v-for="code in dict.furnaceCodes || []" :key="code" :label="code" :value="code" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="manual.ownerName" placeholder="负责人姓名" />
        </el-form-item>
        <el-form-item label="异常原因">
          <el-input v-model="manual.reason" type="textarea" :rows="4" placeholder="描述人工确认的异常工况或风险" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="doManual">发起并冻结投料</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { abnormalOrderApi } from '../api'

const orders = ref([])
const selected = ref(null)
const readiness = ref(null)
const dict = ref({ statuses: [], furnaceCodes: [] })
const statusFilter = ref('')
const loading = ref(false)
const manualDialog = ref(false)
const ownerName = ref('')
const reviewerName = ref('')
const measures = ref('')
const reviewNote = ref('')
const reviewerNote = ref('')
const missingReason = ref('')
const manual = reactive({ furnaceCode: '', ownerName: '', reason: '' })
let timer = null

const canStart = computed(() => selected.value && ['PENDING', 'PROCESSING'].includes(selected.value.status))
const canMeasure = computed(() => selected.value && ['PENDING', 'PROCESSING'].includes(selected.value.status))
const canRequestReview = computed(() => selected.value && selected.value.status === 'PROCESSING')
const canReview = computed(() => selected.value && selected.value.status === 'REVIEWING')
const canRecover = computed(() => canReview.value && readiness.value && readiness.value.recoverable)

function num(v, d = 1) {
  if (v === null || v === undefined) return '--'
  const n = Number(v)
  return Number.isNaN(n) ? '--' : n.toLocaleString('zh-CN', { minimumFractionDigits: d, maximumFractionDigits: d })
}
function time(v) {
  return v ? String(v).replace('T', ' ').slice(0, 16) : '--'
}
function sourceName(code) {
  return { AUTO_COMBUSTION: '炉况自动', AUTO_EMISSION: '排放自动', MANUAL: '人工发起' }[code] || code
}
function attachedName(code) {
  return { INITIAL: '初始证据', MERGED: '并发合并', REBOUND: '恢复期间反弹' }[code] || code
}
function eventName(code) {
  return {
    ORDER_CREATED: '创建处置单',
    AUTO_COMBUSTION_MERGED: '自动炉况证据合并',
    AUTO_EMISSION_MERGED: '自动告警证据合并',
    MANUAL_MERGED: '人工并发合并',
    MANUAL_REBOUND: '人工证据反弹',
    REBOUND_ALARM: '严重告警反弹',
    START_PROCESSING: '开始处置',
    SAVE_MEASURES: '记录措施',
    REQUEST_REVIEW: '申请复核',
    RECOVERED: '复核恢复',
    WAIT_MISSING_DATA: '继续冻结等待补齐',
    CLOSE_KEEP_FROZEN: '关闭但保持冻结',
    CLOSE: '正常关闭',
    FEED_BLOCKED: '投料联锁拦截',
    RECOVER_REJECTED: '恢复被拒绝',
    STALE_VERSION_REJECTED: '过期版本拒绝'
  }[code] || code
}
function statusTag(status) {
  if (status === 'PENDING') return 'warn'
  if (status === 'PROCESSING') return 'primary'
  if (status === 'REVIEWING') return 'warn'
  if (status === 'RECOVERED') return 'ok'
  return 'primary'
}
function stepIndex(status) {
  return ['PENDING', 'PROCESSING', 'REVIEWING', 'RECOVERED', 'CLOSED'].indexOf(status)
}

async function loadAll() {
  await Promise.all([loadList(), loadDict()])
  if (selected.value) await refreshSelected(selected.value.id)
}
async function loadList() {
  try {
    const res = await abnormalOrderApi.list(statusFilter.value || undefined)
    orders.value = res.data || []
  } catch (e) {
    ElMessage.error(e.message)
  }
}
async function loadDict() {
  try {
    const res = await abnormalOrderApi.dict()
    dict.value = res.data || {}
  } catch (e) {
    ElMessage.error(e.message)
  }
}
async function selectOrder(o) {
  await refreshSelected(o.id)
}
async function refreshSelected(id) {
  try {
    const [detailRes, readyRes] = await Promise.all([
      abnormalOrderApi.detail(id),
      abnormalOrderApi.readiness(id).catch(() => ({ data: null }))
    ])
    selected.value = detailRes.data
    readiness.value = readyRes.data
    ownerName.value = selected.value.ownerName && selected.value.ownerName !== '系统自动' ? selected.value.ownerName : ownerName.value
    measures.value = selected.value.measures || ''
    reviewNote.value = selected.value.reviewRequestNote || ''
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function scanSevere() {
  await withLock(async () => {
    const res = await abnormalOrderApi.scanSevere()
    ElMessage.success(res.message || '已同步')
  })
}

function openManual() {
  manual.furnaceCode = dict.value.furnaceCodes?.[0] || ''
  manual.ownerName = ''
  manual.reason = ''
  manualDialog.value = true
}
async function withLock(action) {
  loading.value = true
  try {
    await action()
    await loadList()
    if (selected.value) await refreshSelected(selected.value.id)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}
async function doManual() {
  await withLock(async () => {
    const res = await abnormalOrderApi.manual({ ...manual })
    ElMessage.success(res.message || '已发起')
    manualDialog.value = false
    await refreshSelected(res.data.id)
  })
}
async function doStart() {
  await withLock(async () => {
    const res = await abnormalOrderApi.start(selected.value.id, {
      expectedVersion: selected.value.version,
      ownerName: ownerName.value
    })
    ElMessage.success(res.message || '已开始')
    await refreshSelected(res.data.id)
  })
}
async function doSaveMeasures() {
  await withLock(async () => {
    const res = await abnormalOrderApi.measures(selected.value.id, {
      expectedVersion: selected.value.version,
      ownerName: ownerName.value,
      measures: measures.value
    })
    ElMessage.success(res.message || '已保存')
    await refreshSelected(res.data.id)
  })
}
async function doRequestReview() {
  await withLock(async () => {
    const res = await abnormalOrderApi.requestReview(selected.value.id, {
      expectedVersion: selected.value.version,
      ownerName: ownerName.value,
      measures: measures.value,
      note: reviewNote.value
    })
    ElMessage.success(res.message || '已申请复核')
    await refreshSelected(res.data.id)
  })
}
async function toggleMissing(alarm) {
  await withLock(async () => {
    const res = await abnormalOrderApi.markDataMissing(selected.value.id, alarm.alarmId, !alarm.forceMissingData, '值班员')
    ElMessage.success(res.message || '已更新')
    await refreshSelected(res.data.id)
  })
}
async function loadReadiness() {
  if (!selected.value) return
  await refreshSelected(selected.value.id)
}
async function doReview(choice) {
  if (choice === 'CLOSE_KEEP_FROZEN' && !missingReason.value.trim()) {
    ElMessage.error('选择关闭但保持冻结时必须记录告警数据缺失理由')
    return
  }
  if (choice === 'WAIT' && !(reviewerNote.value || '').trim()) {
    ElMessage.error('请记录继续冻结等待补齐的说明')
    return
  }
  await withLock(async () => {
    const res = await abnormalOrderApi.review(selected.value.id, {
      expectedVersion: selected.value.version,
      reviewerName: reviewerName.value,
      choice,
      reviewNote: reviewerNote.value,
      missingReason: missingReason.value
    })
    ElMessage.success(res.message || '复核处理完成')
    await refreshSelected(res.data.id)
  })
}
async function doClose() {
  try {
    await ElMessageBox.confirm('确认该处置单已复核恢复并关闭？关闭后归档，不影响已解除的投料权限。', '关闭确认', { type: 'warning' })
  } catch {
    return
  }
  await withLock(async () => {
    const res = await abnormalOrderApi.close(selected.value.id, {
      expectedVersion: selected.value.version,
      operatorName: reviewerName.value || ownerName.value,
      closeNote: reviewerNote.value || '复核恢复后关闭'
    })
    ElMessage.success(res.message || '已关闭')
    await refreshSelected(res.data.id)
  })
}

onMounted(async () => {
  await loadAll()
  timer = setInterval(async () => {
    await loadList()
    if (selected.value) await refreshSelected(selected.value.id)
  }, 10000)
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.wp-table tr.active td {
  background: rgba(41, 182, 246, 0.12);
}
.reason-cell {
  max-width: 280px;
  color: var(--wp-text-dim);
}
.order-steps {
  padding: 18px 10px 8px;
}
.lock-alert {
  margin: 8px 0 12px;
}
.action-grid,
.evidence-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.evidence-grid {
  grid-template-columns: 0.9fr 1.25fr 0.85fr;
}
.action-card .sub {
  margin-left: 8px;
  color: var(--wp-text-mute);
  font-size: 11px;
}
.button-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.readiness-box {
  margin-bottom: 10px;
  padding: 10px;
  border: 1px solid var(--wp-border);
  border-radius: 6px;
  background: #101a21;
}
.ready-row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 12px;
  color: var(--wp-text-dim);
}
.ok-text { color: var(--wp-success, #2ecc8f); }
.bad-text { color: var(--wp-danger); }
.impact {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--wp-border);
  font-size: 12px;
  color: var(--wp-warn);
}
.block-list {
  margin: 8px 0 0 18px;
  color: #ffb4b0;
  font-size: 12px;
}
.kv {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px dashed #1e2b34;
  font-size: 12px;
  color: var(--wp-text-dim);
}
.kv b {
  color: var(--wp-text);
}
.snapshot-alert {
  margin-top: 8px;
}
.compact {
  font-size: 11px;
}
.grab-row {
  display: grid;
  grid-template-columns: 80px 1fr auto auto;
  gap: 8px;
  align-items: center;
  padding: 7px 0;
  border-bottom: 1px dashed #1e2b34;
  font-size: 12px;
  color: var(--wp-text-dim);
}
.event-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}
.dim {
  color: var(--wp-text-mute);
}
.fail-reason {
  margin-top: 4px;
  color: #ffb4b0;
}
.empty {
  text-align: center;
  color: var(--wp-text-mute);
  padding: 20px 0 !important;
}
@media (max-width: 1100px) {
  .action-grid,
  .evidence-grid {
    grid-template-columns: 1fr;
  }
}
</style>
