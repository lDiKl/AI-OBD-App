import axios from 'axios'
import { getAuth } from 'firebase/auth'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8000',
})

// Attach Firebase ID token to every request
client.interceptors.request.use(async (config) => {
  const auth = getAuth()
  const token = await auth.currentUser?.getIdToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default client
