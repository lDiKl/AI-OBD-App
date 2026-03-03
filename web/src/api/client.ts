import axios from 'axios'
import { getAuth } from 'firebase/auth'

const client = axios.create({
  // Empty baseURL → relative paths → Vite proxy routes /api/* to API container
  baseURL: '',
})

// Attach Firebase ID token to every request
client.interceptors.request.use(async (config) => {
  const auth = getAuth()
  const token = await auth.currentUser?.getIdToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default client
