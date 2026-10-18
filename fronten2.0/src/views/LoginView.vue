<template>
  <main class="login-page">
    <section class="login-intro" aria-label="DigiLedger">
      <div class="login-brand"><img src="@/assets/digiledger-logo.png" alt="" /><span>DIGI LEDGER</span></div>
      <div class="login-story"><span class="login-eyebrow">YOUR PERSONAL COLLECTION</span><h1>每一件热爱，<br />都有迹可循。</h1><p>你的设备、投入与心愿，<br />收藏在自己的数字仓库。</p></div>
      <span class="login-footnote">DigiLedger · 私人物品库</span>
    </section>
    <section class="login-access">
      <form class="login-form" @submit.prevent="submit">
        <span class="login-eyebrow">PRIVATE ACCESS</span>
        <h2>打开我的物品库</h2>
        <p class="login-description">输入访问密码，继续管理你的数码收藏。</p>
        <label for="access-password">访问密码</label>
        <input id="access-password" v-model="password" type="password" autocomplete="current-password"
          placeholder="请输入密码" required maxlength="256" :disabled="busy || !configured" aria-describedby="login-message" />
        <p id="login-message" class="login-message" role="status" aria-live="polite">{{ message }}</p>
        <button class="login-submit" type="submit" :disabled="busy || checking || !configured">{{ busy ? '正在登录…' : '进入物品库 →' }}</button>
        <button v-if="loadFailed" class="login-retry" type="button" @click="load">重新连接</button>
        <p class="login-caption">仅供自己使用，一道密码即可进入。</p>
      </form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authError, checkLogin, login, safeRedirect } from '@/auth'
const router = useRouter(), route = useRoute()
const password = ref(''), message = ref(''), busy = ref(false), checking = ref(true)
const configured = ref(false), loadFailed = ref(false)
async function load() {
  checking.value = true; loadFailed.value = false; message.value = ''
  try {
    const status = await checkLogin()
    configured.value = status.configured
    if (!status.configured) message.value = '尚未设置访问密码。请在服务器配置 DL_AUTH_PASSWORD 后重启服务。'
  } catch (error) { message.value = authError(error); loadFailed.value = true }
  finally { checking.value = false }
}
async function submit() {
  if (busy.value || !configured.value) return
  busy.value = true; message.value = ''
  try {
    await login(password.value)
    password.value = ''
    await router.replace(safeRedirect(route.query.redirect))
  } catch (error) { message.value = authError(error) }
  finally { busy.value = false }
}
onMounted(load)
</script>

<style scoped>
.login-page{min-height:100vh;display:grid;grid-template-columns:1fr 1fr;background:#f5f6f3;color:#171915;font-family:"Microsoft YaHei",sans-serif}
.login-intro{display:flex;flex-direction:column;justify-content:space-between;min-height:100vh;padding:48px 12%;background:#0f100e;color:#fff}
.login-brand{display:flex;align-items:center;gap:14px;letter-spacing:.12em;font-size:15px;font-weight:700}.login-brand img{width:48px;height:48px;object-fit:contain}
.login-story{padding:60px 0}.login-eyebrow{font-size:10px;font-weight:700;letter-spacing:.18em;color:#70766a}.login-intro .login-eyebrow{color:#b7ff3c}
.login-story h1{font-size:clamp(30px,3.4vw,52px);font-weight:500;line-height:1.5;letter-spacing:-.03em;margin:26px 0}.login-story p{color:#a4aa9c;font-size:14px;line-height:2.1}.login-footnote{color:#8c9384;font-size:11px}
.login-access{display:flex;align-items:center;justify-content:center;padding:40px}.login-form{width:100%;max-width:350px}.login-form h2{font-size:27px;letter-spacing:-.04em;margin:18px 0 12px}.login-description{color:#70766a;font-size:13px;line-height:1.8;margin-bottom:42px}
.login-form label{display:block;font-size:12px;font-weight:700;margin-bottom:12px}.login-form input{box-sizing:border-box;width:100%;height:52px;border:1px solid #c9cec1;border-radius:12px;padding:0 16px;background:#fff;font-size:16px;color:#171915}.login-form input:focus{outline:2px solid #627f34;outline-offset:3px}
.login-message{min-height:42px;margin:12px 0;font-size:12px;color:#b23636;line-height:1.7}.login-submit{width:100%;height:52px;background:#b7ff3c;border:1px solid #94cf30;border-radius:12px;font-size:14px;font-weight:700;cursor:pointer;color:#171915}.login-submit:hover{background:#a8ec32}.login-submit:disabled{opacity:.5;cursor:wait}.login-caption{text-align:center;color:#70766a;font-size:11px;margin-top:24px}.login-retry{display:block;margin:18px auto;border:0;background:transparent;text-decoration:underline;cursor:pointer;color:#4e534b}
@media(max-width:700px){.login-page{grid-template-columns:1fr}.login-intro{min-height:auto;padding:24px}.login-story,.login-footnote{display:none}.login-brand img{width:36px;height:36px}.login-access{padding:56px 28px}.login-form h2{font-size:25px}}
</style>
