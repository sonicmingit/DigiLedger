import type { CategoryNode, DictionaryTag } from "@/services/api";

/**
 * 将后端树形字典展开为移动端 picker / 列表可直接消费的顺序。
 * pathLabel 保留完整层级，避免手机端仅显示叶子名称时产生同名歧义。
 */
export function flattenTree<T extends CategoryNode | DictionaryTag>(
  nodes: T[],
  parentPath = "",
): Array<T & { pathLabel: string; depth: number }> {
  return nodes.flatMap((node) => {
    const pathLabel = parentPath ? `${parentPath} / ${node.name}` : node.name;
    const current = {
      ...node,
      pathLabel,
      depth: pathLabel.split(" / ").length - 1,
    };
    const children = (node.children || []) as T[];
    return [current, ...flattenTree(children, pathLabel)];
  });
}

/** 根据叶子分类 ID 返回完整中文路径，避免把后端的内部 ID 路径展示给用户。 */
export function categoryPathLabel(nodes: CategoryNode[], categoryId?: number) {
  if (!categoryId) return "";
  return flattenTree(nodes).find((node) => node.id === categoryId)?.pathLabel || "";
}
