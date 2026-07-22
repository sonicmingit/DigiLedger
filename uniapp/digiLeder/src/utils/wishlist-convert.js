export function buildWishlistConvertPayload(item = {}) {
	const name = String(item.name || '').trim();
	if (!name) {
		throw new Error('心愿名称为空，无法转资产');
	}

	const budget = Number(item.budget || item.targetCostValue || 0);
	const targetCostValue = Number.isFinite(budget) && budget > 0 ? budget : 0;
	const purchaseDate = item.purchaseDate || new Date().toISOString().slice(0, 10);

	return {
		name,
		status: item.status || '使用中',
		purchaseDate,
		targetCostValue,
		targetCostStrategy: item.targetCostStrategy || 'WISHLIST_BUDGET',
		purchases: targetCostValue > 0
			? [{ type: 'PRIMARY', price: targetCostValue, purchaseDate, quantity: 1 }]
			: []
	};
}
