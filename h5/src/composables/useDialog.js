import { reactive } from 'vue'

const state = reactive({
  open: false,
  mode: 'confirm',
  title: '',
  message: '',
  tone: 'default'
})

let settle = null

function open(options) {
  if (settle) settle(false)
  Object.assign(state, { open: true, mode: 'confirm', title: '', message: '', tone: 'default', ...options })
  return new Promise(resolve => { settle = resolve })
}

function close(result) {
  if (!state.open) return
  state.open = false
  const resolve = settle
  settle = null
  resolve?.(result)
}

export function confirmDialog(message, options = {}) {
  return open({ mode: 'confirm', title: '请确认操作', message, tone: 'warning', ...options })
}

export function alertDialog(message, options = {}) {
  return open({ mode: 'alert', title: '操作提示', message, tone: 'error', ...options })
}

export function useDialog() {
  return { state, close, confirm: confirmDialog, alert: alertDialog }
}
