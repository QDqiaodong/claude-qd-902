import { createRouter, createWebHistory } from 'vue-router'

import IntakeView from '../views/IntakeView.vue'
import PitView from '../views/PitView.vue'
import FurnaceView from '../views/FurnaceView.vue'
import EmissionView from '../views/EmissionView.vue'
import AbnormalOrderView from '../views/AbnormalOrderView.vue'

const routes = [
  { path: '/', redirect: '/intake' },
  { path: '/intake', name: 'intake', component: IntakeView, meta: { title: '进厂与计量' } },
  { path: '/pit', name: 'pit', component: PitView, meta: { title: '垃圾储坑' } },
  { path: '/furnace', name: 'furnace', component: FurnaceView, meta: { title: '焚烧炉与余热发电' } },
  { path: '/abnormal', name: 'abnormal', component: AbnormalOrderView, meta: { title: '异常工况处置单' } },
  { path: '/emission', name: 'emission', component: EmissionView, meta: { title: '环保排放与灰渣' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, saved) {
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }
    return saved || { top: 0 }
  }
})

export default router
