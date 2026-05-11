import { useAuthStore } from '@/stores/auth'

export default {
  mounted(el, binding) {
    const authStore = useAuthStore()
    const permission = binding.value
    if (permission && !authStore.hasPermission(permission)) {
      el.parentNode?.removeChild(el)
    }
  }
}
