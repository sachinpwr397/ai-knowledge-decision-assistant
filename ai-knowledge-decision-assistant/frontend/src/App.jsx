import React, { useEffect, useState } from 'react'
import { api, apiBase, clearSession, getToken, setSession } from './lib/api'
import { Activity, ArrowRight, Bot, Check, ChevronRight, Copy, FileText, LogOut, MessageCircle, Plus, RefreshCw, ShieldCheck, Sparkles, Trash2, UploadCloud, X } from 'lucide-react'

function App() {
  const isWidget = window.location.pathname.startsWith('/widget')
  if (isWidget) return <ChatWidget token={new URLSearchParams(window.location.search).get('token') || ''} />
  return <AdminApp />
}

function AdminApp() {
  const [profile, setProfile] = useState(() => { try { return JSON.parse(localStorage.getItem('profile') || 'null') } catch { return null } })
  const [checking, setChecking] = useState(Boolean(getToken()))
  const [authMode, setAuthMode] = useState('login')
  const [error, setError] = useState('')

  useEffect(() => {
    if (!getToken()) { setChecking(false); return }
    api('/api/auth/me').then(p => setProfile(p)).catch(() => { clearSession(); setProfile(null) }).finally(() => setChecking(false))
  }, [])

  async function authenticate(form) {
    setError('')
    try {
      const endpoint = authMode === 'register' ? '/api/auth/register' : '/api/auth/login'
      const data = await api(endpoint, { method: 'POST', body: JSON.stringify(form) })
      setSession(data); setProfile(data)
    } catch (e) { setError(e.message) }
  }
  function logout() { clearSession(); setProfile(null); setAuthMode('login') }

  if (checking) return <div className="center-screen"><div className="loader" />Checking your session…</div>
  return profile ? <Dashboard profile={profile} onLogout={logout} /> : <AuthScreen mode={authMode} setMode={setAuthMode} onSubmit={authenticate} error={error} />
}

function AuthScreen({ mode, setMode, onSubmit, error }) {
  const [form, setForm] = useState({ organizationName: '', fullName: '', email: '', password: '' })
  const [busy, setBusy] = useState(false)
  const update = e => setForm(v => ({ ...v, [e.target.name]: e.target.value }))
  async function submit(e) { e.preventDefault(); setBusy(true); await onSubmit(form); setBusy(false) }
  return <main className="auth-shell">
    <section className="auth-aside">
      <div className="brand"><span className="brand-icon"><Sparkles size={19}/></span><span>Knowwise</span></div>
      <div className="hero-copy"><span className="eyebrow">AI KNOWLEDGE WORKSPACE</span><h1>Your documents.<br/><em>Your answers.</em></h1><p>Turn internal PDFs into a grounded assistant that understands policy, evaluates decisions and cites its sources.</p></div>
      <div className="feature-list"><div><ShieldCheck size={17}/> Private knowledge per organization</div><div><FileText size={17}/> Answers with page-level references</div><div><Activity size={17}/> Structured decisions, not guesswork</div></div>
      <div className="aside-footer">Built for teams that need answers they can verify.</div>
    </section>
    <section className="auth-panel"><div className="auth-card">
      <span className="eyebrow">{mode === 'register' ? 'GET STARTED' : 'WELCOME BACK'}</span>
      <h2>{mode === 'register' ? 'Create your workspace' : 'Sign in to your workspace'}</h2>
      <p className="muted">{mode === 'register' ? 'Set up your organization’s private AI assistant.' : 'Continue to your organization dashboard.'}</p>
      <form onSubmit={submit} className="form-stack">
        {mode === 'register' && <><label>Organization name<input name="organizationName" required maxLength="150" placeholder="Acme University" value={form.organizationName} onChange={update}/></label><label>Your name<input name="fullName" required maxLength="120" placeholder="Alex Morgan" value={form.fullName} onChange={update}/></label></>}
        <label>Email address<input type="email" name="email" required placeholder="you@organization.com" value={form.email} onChange={update}/></label>
        <label>Password<input type="password" name="password" required minLength="8" placeholder="At least 8 characters" value={form.password} onChange={update}/></label>
        {error && <div className="alert error">{error}</div>}
        <button className="primary-btn" disabled={busy}>{busy ? 'Please wait…' : mode === 'register' ? 'Create workspace' : 'Sign in'}<ArrowRight size={17}/></button>
      </form>
      <div className="auth-switch">{mode === 'register' ? 'Already have a workspace?' : 'New to Knowwise?'} <button onClick={() => setMode(mode === 'register' ? 'login' : 'register')}>{mode === 'register' ? 'Sign in' : 'Create an account'}</button></div>
      <div className="secure-note"><ShieldCheck size={14}/> Passwords are encrypted. Organization documents stay isolated.</div>
    </div></section>
  </main>
}

function Dashboard({ profile, onLogout }) {
  const [documents, setDocuments] = useState([])
  const [assistant, setAssistant] = useState(null)
  const [usage, setUsage] = useState({ documents: 0, questionsAsked: 0, questionsAnswered: 0, questionsUnanswered: 0 })
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [selectedFile, setSelectedFile] = useState(null)

  async function refresh() {
    try {
      const [docs, asst, stats] = await Promise.all([api('/api/documents'), api('/api/organization/assistant'), api('/api/organization/usage')])
      setDocuments(docs); setAssistant(asst); setUsage(stats)
    } catch (e) { setError(e.message) }
  }
  useEffect(() => { refresh() }, [])

  async function upload(e) {
    e.preventDefault(); if (!selectedFile) return
    setBusy(true); setError(''); setMessage('')
    const data = new FormData(); data.append('file', selectedFile)
    try { await api('/api/documents', { method: 'POST', body: data }); setSelectedFile(null); e.target.reset(); setMessage('PDF uploaded and indexed successfully.'); await refresh() }
    catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  async function removeDocument(id) {
    if (!confirm('Delete this PDF and all of its indexed text?')) return
    try { await api(`/api/documents/${id}`, { method: 'DELETE' }); setMessage('Document deleted.'); await refresh() } catch (e) { setError(e.message) }
  }
  async function regenerate() {
    if (!confirm('Regenerate the public assistant link? The old link and embed code will stop working.')) return
    try { setAssistant(await api('/api/organization/assistant/regenerate', { method: 'POST' })); setMessage('Assistant token regenerated.') } catch (e) { setError(e.message) }
  }
  async function copy(value, label) { try { await navigator.clipboard.writeText(value); setMessage(`${label} copied to clipboard.`) } catch { setError('Clipboard access was blocked by your browser.') } }
  const formatBytes = n => n < 1024 * 1024 ? `${Math.round(n / 1024)} KB` : `${(n / 1024 / 1024).toFixed(1)} MB`

  return <div className="dashboard-shell">
    <header className="topbar"><div className="brand dark"><span className="brand-icon"><Sparkles size={18}/></span><span>Knowwise</span></div><div className="topbar-right"><span className="workspace-pill"><span className="online-dot"/>{profile.organizationName || 'Workspace'}</span><button className="icon-btn" title="Sign out" onClick={onLogout}><LogOut size={17}/></button></div></header>
    <main className="dashboard-main">
      <div className="page-heading"><div><span className="eyebrow">WORKSPACE OVERVIEW</span><h1>Good to see you, {profile.fullName?.split(' ')[0] || 'there'}.</h1><p>Manage your organization's knowledge and AI assistant.</p></div><div className="status-chip"><span className="online-dot"/> Workspace active</div></div>
      {message && <div className="alert success"><Check size={16}/>{message}<button onClick={() => setMessage('')}><X size={14}/></button></div>}
      {error && <div className="alert error"><X size={16}/>{error}<button onClick={() => setError('')}><X size={14}/></button></div>}
      <section className="stats-grid">
        <StatCard label="Knowledge documents" value={usage.documents} detail="PDFs indexed" icon={<FileText/>}/>
        <StatCard label="Questions asked" value={usage.questionsAsked} detail="All-time usage" icon={<MessageCircle/>}/>
        <StatCard label="Answered" value={usage.questionsAnswered} detail="With retrieved evidence" icon={<Check/>}/>
        <StatCard label="Needs more evidence" value={usage.questionsUnanswered} detail="No relevant source found" icon={<Activity/>}/>
      </section>
      <div className="content-grid">
        <section className="panel knowledge-panel">
          <div className="panel-heading"><div><span className="eyebrow">YOUR KNOWLEDGE BASE</span><h2>Documents</h2><p>Upload PDFs to give your assistant trusted source material.</p></div><span className="count-pill">{documents.length} / 10</span></div>
          <form onSubmit={upload} className="upload-box">
            <div className="upload-icon"><UploadCloud size={22}/></div><div className="upload-copy"><strong>Upload a PDF document</strong><span>PDF only · Up to 10 MB · Maximum 20 pages per file</span></div>
            <label className="secondary-btn file-pick">Browse files<input type="file" accept="application/pdf,.pdf" onChange={e => setSelectedFile(e.target.files?.[0] || null)} hidden/></label>
            {selectedFile && <div className="selected-file"><FileText size={15}/>{selectedFile.name}<span>{formatBytes(selectedFile.size)}</span></div>}
            <button className="primary-btn upload-submit" disabled={!selectedFile || busy || documents.length >= 10}>{busy ? 'Processing PDF…' : <><Plus size={16}/> Add to knowledge base</>}</button>
          </form>
          <div className="document-list">
            {documents.length === 0 ? <div className="empty-state"><FileText size={28}/><strong>No documents yet</strong><span>Upload your first PDF to start building your assistant's knowledge base.</span></div> : documents.map(doc => <div className="document-row" key={doc.id}><div className="pdf-icon">PDF</div><div className="doc-info"><strong>{doc.filename}</strong><span>{doc.pages} pages · {doc.chunks} text chunks · {formatBytes(doc.sizeBytes)}</span></div><button className="icon-btn danger-icon" title="Delete document" onClick={() => removeDocument(doc.id)}><Trash2 size={16}/></button></div>)}
          </div>
        </section>
        <section className="panel assistant-panel">
          <div className="panel-heading"><div><span className="eyebrow">PUBLIC EXPERIENCE</span><h2>Your assistant</h2><p>Share a secure link or embed the assistant on your website.</p></div><div className="assistant-avatar"><Bot size={22}/></div></div>
          {assistant && <>
            <div className="assistant-preview"><div className="preview-top"><span className="preview-dot"/><span className="preview-dot"/><span className="preview-dot"/><span className="preview-title">{assistant.organizationName} Assistant</span></div><div className="preview-body"><div className="bot-bubble"><Sparkles size={14}/> Hello! Ask me about the documents in {assistant.organizationName}'s knowledge base.</div><div className="user-bubble">What are the key eligibility requirements?</div><div className="bot-bubble muted-bubble">I'll search the organization's documents and cite the relevant pages.</div></div></div>
            <div className="share-field"><label>Assistant URL</label><div className="copy-field"><span>{assistant.assistantUrl}</span><button onClick={() => copy(assistant.assistantUrl,'Assistant URL')}><Copy size={15}/></button></div></div>
            <div className="share-field"><label>Embed code</label><div className="code-field"><code>{assistant.embedCode}</code><button onClick={() => copy(assistant.embedCode,'Embed code')}><Copy size={15}/></button></div></div>
            <div className="assistant-actions"><a className="secondary-btn" href={assistant.assistantUrl} target="_blank" rel="noreferrer"><MessageCircle size={15}/> Open assistant</a><button className="text-btn" onClick={regenerate}><RefreshCw size={14}/> Regenerate link</button></div>
          </>}
          {!assistant && <div className="empty-state"><Bot size={28}/><span>Loading assistant settings…</span></div>}
        </section>
      </div>
      <section className="info-banner"><div className="info-icon"><ShieldCheck size={20}/></div><div><strong>Organization data is isolated</strong><p>Every query is resolved against this workspace's documents only. Public assistant tokens cannot access the admin dashboard or private PDF files.</p></div><ChevronRight size={18}/></section>
      <footer className="dashboard-footer">Knowwise · AI Knowledge &amp; Decision Assistant <span>Powered by grounded retrieval and structured decisions</span></footer>
    </main>
  </div>
}

function StatCard({ label, value, detail, icon }) { return <div className="stat-card"><div className="stat-top"><span>{label}</span><span className="stat-icon">{icon}</span></div><strong>{value}</strong><small>{detail}</small></div> }

function ChatWidget({ token }) {
  const [assistant, setAssistant] = useState(null)
  const [question, setQuestion] = useState('')
  const [messages, setMessages] = useState([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [sessionError, setSessionError] = useState('')
  const endRef = React.useRef(null)
  useEffect(() => { if (!token) { setSessionError('This assistant link is missing its token.'); return } fetch(`${apiBase}/api/public/assistant/${encodeURIComponent(token)}`).then(async r => { const d=await r.json(); if(!r.ok) throw new Error(d.error || 'Assistant not found'); setAssistant(d); setMessages([{role:'assistant',content:d.welcomeMessage}]) }).catch(e => setSessionError(e.message)) }, [token])
  useEffect(() => { endRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages])
  async function send(e) {
    e.preventDefault(); const q=question.trim(); if(!q || busy || !token)return
    setQuestion('');setError('');setMessages(v=>[...v,{role:'user',content:q}]);setBusy(true)
    try { const r=await fetch(`${apiBase}/api/public/ask`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({assistantToken:token,question:q})});const d=await r.json();if(!r.ok)throw new Error(d.error||'Unable to answer');setMessages(v=>[...v,{role:'assistant',content:d.answer,sources:d.sources||[],decisions:d.decisions||[],answered:d.answered}]) }
    catch(err){setError(err.message)}finally{setBusy(false)}
  }
  return <div className="widget-page"><div className="chat-card"><header className="chat-header"><div className="chat-brand-icon"><Bot size={19}/></div><div><strong>{assistant?.organizationName || 'Knowledge Assistant'}</strong><span><i/> AI assistant · Source-grounded answers</span></div><span className="powered-tag">KNOWWISE</span></header>
    <div className="chat-messages">
      {sessionError && <div className="widget-error">{sessionError}</div>}
      {messages.map((m,i)=><div className={`message-row ${m.role}`} key={i}><div className={`message-avatar ${m.role}`}>{m.role==='assistant'?<Sparkles size={14}/>:<span>Y</span>}</div><div className="message-content"><div className={`message-bubble ${m.role}`}>{m.content}</div>{m.decisions?.length>0&&<div className="decision-list">{m.decisions.map((d,j)=><div className="decision-chip" key={j}><b>{d.type}</b><span>{d.outcome}</span></div>)}</div>}{m.sources?.length>0&&<div className="source-list"><span className="source-heading">SOURCES</span>{m.sources.map((s,j)=><div className="source-item" key={j}><FileText size={12}/><span>{s.document} · Page {s.page}</span></div>)}</div>}</div></div>)}
      {busy&&<div className="message-row assistant"><div className="message-avatar assistant"><Sparkles size={14}/></div><div className="typing"><i/><i/><i/></div></div>}
      {error&&<div className="widget-error">{error}</div>}<div ref={endRef}/>
    </div>
    <form className="chat-input-area" onSubmit={send}><div className="chat-input-wrap"><input value={question} onChange={e=>setQuestion(e.target.value)} placeholder="Ask a question about these documents…" disabled={!assistant||busy}/><button disabled={!question.trim()||busy||!assistant} aria-label="Send question"><ArrowRight size={18}/></button></div><div className="chat-disclaimer">Answers are generated from the organization's uploaded documents. Verify important decisions against the original policy.</div></form>
  </div><div className="widget-footer">Secure knowledge assistant <span>·</span> Powered by Knowwise</div></div>
}

export default App
