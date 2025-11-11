import { computed, ref } from 'vue'
import {
  fetchCategoryTree,
  fetchPlatforms,
  fetchTagTree,
  type CategoryNode,
  type PlatformItem,
  type TagNode
} from '@/api/dict'

const categoryTree = ref<CategoryNode[]>([])
const tagTree = ref<TagNode[]>([])
const platforms = ref<PlatformItem[]>([])
const loaded = ref(false)
const loading = ref(false)
let pending: Promise<void> | null = null

const categoryPathMap = computed(() => {
  const map = new Map<number, string>()
  const walk = (nodes: CategoryNode[], parents: string[]) => {
    nodes.forEach((node) => {
      const pathNames = [...parents, node.name]
      map.set(node.id, pathNames.join(' / '))
      if (node.children?.length) {
        walk(node.children, pathNames)
      }
    })
  }
  walk(categoryTree.value, [])
  return map
})

const tagMap = computed(() => {
  const map = new Map<number, TagNode>()
  const walk = (nodes: TagNode[]) => {
    nodes.forEach((node) => {
      map.set(node.id, node)
      if (node.children?.length) {
        walk(node.children)
      }
    })
  }
  walk(tagTree.value)
  return map
})

const fetchAll = async () => {
  if (pending) {
    await pending
    return
  }
  pending = (async () => {
    loading.value = true
    try {
      const [categories, tagResult, platformList] = await Promise.all([
        fetchCategoryTree(),
        fetchTagTree(),
        fetchPlatforms()
      ])
      categoryTree.value = categories
      tagTree.value = tagResult
      platforms.value = platformList
      loaded.value = true
    } finally {
      loading.value = false
      pending = null
    }
  })()
  await pending
}

export const useDictionaries = () => {
  const load = async () => {
    if (loaded.value) return
    await fetchAll()
  }

  const refresh = async () => {
    await fetchAll()
  }

  return {
    load,
    refresh,
    categoryTree,
    tagTree,
    platforms,
    categoryPathMap,
    tagMap
  }
}
