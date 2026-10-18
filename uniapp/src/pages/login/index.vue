<template>
  <view class="login-page">
    <image class="login-logo" src="/static/app-icon.png" />
    <text class="login-brand">DIGI LEDGER</text>
    <text class="login-title">打开我的物品库</text>
    <text class="login-copy">输入访问密码，继续管理你的数码收藏。</text>
    <view class="login-fields">
      <text class="login-label">服务器地址</text>
      <input v-model="server" placeholder="https://server.example.com/api" :disabled="busy" />
      <text class="login-label">访问密码</text>
      <input v-model="password" password placeholder="请输入密码" :maxlength="-1" :disabled="busy" @confirm="submit" />
      <text class="login-error">{{ message }}</text>
      <button class="login-button" :loading="busy" :disabled="busy" @click="submit">进入物品库 →</button>
    </view>
    <text class="login-caption">DigiLedger · 私人物品库</text>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { apiRequest, type LoginStatus } from '@/services/http';
import { getServerProfile, nodeUrl, saveServerProfile } from '@/services/server-profile';
const profile = getServerProfile();
const server = ref(nodeUrl(profile, profile.preferred)), password = ref(''), message = ref(''), busy = ref(false);
onLoad(async () => {
  busy.value = true;
  try {
    const status = await apiRequest<LoginStatus>('/auth/status', { allowFailover: false });
    if (status.authenticated) uni.reLaunch({ url: '/pages/assets/home' });
    else if (!status.configured) message.value = '请先在服务器配置 DL_AUTH_PASSWORD 后重启服务';
  } catch (_) { /* The server address remains editable when the node is unavailable. */ }
  finally { busy.value = false; }
});
async function submit() {
  if (busy.value) return;
  if (!server.value.trim() || !password.value) { message.value = '请填写服务器地址和密码'; return; }
  busy.value = true; message.value = '';
  try {
    const updated = getServerProfile();
    if (updated.preferred === 'primary') updated.primaryUrl = server.value;
    else updated.secondaryUrl = server.value;
    saveServerProfile(updated);
    const status = await apiRequest<LoginStatus>('/auth/status', { allowFailover: false });
    if (!status.configured) throw new Error('请先在服务器配置 DL_AUTH_PASSWORD 后重启服务');
    await apiRequest('/auth/login', { method: 'POST', data: { password: password.value } });
    password.value = '';
    uni.reLaunch({ url: '/pages/assets/home' });
  } catch (error) { message.value = (error as Error).message; }
  finally { busy.value = false; }
}
</script>
<style scoped>
.login-page{min-height:100vh;box-sizing:border-box;padding:100rpx 48rpx 60rpx;background:#f5f6f3;color:#171915;display:flex;flex-direction:column}.login-logo{width:96rpx;height:96rpx}.login-brand{font-size:22rpx;letter-spacing:5rpx;margin-top:24rpx;font-weight:700}.login-title{font-size:48rpx;font-weight:700;margin-top:80rpx}.login-copy{font-size:24rpx;color:#70766a;margin-top:20rpx;line-height:1.8}.login-fields{margin-top:56rpx}.login-label{display:block;font-size:24rpx;font-weight:700;margin:28rpx 0 18rpx}.login-fields input{height:100rpx;border:1px solid #c9cec1;border-radius:20rpx;padding:0 28rpx;background:#fff;font-size:28rpx}.login-error{display:block;min-height:72rpx;margin-top:20rpx;color:#b23636;font-size:24rpx;line-height:1.6}.login-button{background:#b7ff3c;border-radius:20rpx;font-size:28rpx;font-weight:700}.login-caption{margin-top:80rpx;font-size:22rpx;color:#70766a;text-align:center}
</style>
