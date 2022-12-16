import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useVersionStore = defineStore('version', () => {
  const version = ref(null);
  return { version };
})
