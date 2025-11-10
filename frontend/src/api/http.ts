import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})

http.interceptors.response.use(
  (resp) => {
    const { code, data, msg } = resp.data
    if (code !== 200) return Promise.reject(new Error(msg || 'Request Error'))
    return data
  },
  (err) => Promise.reject(err)
)

export default http
