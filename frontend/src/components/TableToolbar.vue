<template>
  <div class="dl-table-toolbar">
    <div class="dl-table-toolbar__left">
      <slot name="left" />
    </div>
    <div class="dl-table-toolbar__right">
      <slot name="right" />
    </div>
  </div>
</template>

<style scoped>
.dl-table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--dl-space-4);
  margin-bottom: var(--dl-space-3);
}

.dl-table-toolbar__left,
.dl-table-toolbar__right {
  display: flex;
  align-items: center;
  gap: var(--dl-space-2);
  flex-wrap: wrap;
}
</style>

