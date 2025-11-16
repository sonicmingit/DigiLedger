import { ElMessage, ElMessageBox } from 'element-plus'
import { createBrand, createPlatform, createTag } from '@/api/dict'
import { useDictionaries } from '@/composables/useDictionaries'

interface CreatorResult {
  id: number
  name: string
}

/**
 * 字典创建工具，统一处理弹窗与刷新逻辑
 */
export const useDictionaryCreator = () => {
  const { refresh } = useDictionaries()

  /**
   * 通用输入弹窗
   */
  const promptInput = async (title: string, placeholder: string) => {
    try {
      const { value } = await ElMessageBox.prompt('请输入名称', title, {
        confirmButtonText: '创建',
        cancelButtonText: '取消',
        inputPlaceholder: placeholder,
        inputValidator: (val: string) => {
          if (!val || !val.trim().length) {
            return '名称不能为空'
          }
          if (val.trim().length > 50) {
            return '名称长度不能超过 50 字'
          }
          return true
        }
      })
      return value.trim()
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        return null
      }
      throw error
    }
  }

  /**
   * 创建平台
   */
  const promptPlatformCreation = async (): Promise<CreatorResult | null> => {
    const name = await promptInput('新建平台', '例如：闲鱼')
    if (!name) return null
    const id = await createPlatform({ name })
    await refresh()
    ElMessage.success('平台已创建')
    return { id, name }
  }

  /**
   * 创建品牌
   */
  const promptBrandCreation = async (): Promise<CreatorResult | null> => {
    const name = await promptInput('新建品牌', '例如：Apple')
    if (!name) return null
    const id = await createBrand({ name })
    await refresh()
    ElMessage.success('品牌已创建')
    return { id, name }
  }

  /**
   * 创建标签，可选传入父级
   */
  const promptTagCreation = async (parentId: number | null = null): Promise<CreatorResult | null> => {
    const name = await promptInput('新建标签', '例如：主力设备')
    if (!name) return null
    const id = await createTag({ name, parentId })
    await refresh()
    ElMessage.success('标签已创建')
    return { id, name }
  }

  return {
    promptPlatformCreation,
    promptBrandCreation,
    promptTagCreation
  }
}
