using System;
using System.Windows.Forms;

namespace UnicodeToNudi
{
    /// <summary>
    /// Invisible window that owns the global keyboard shortcut.
    /// Windows sends WM_HOTKEY here whichever program is in front.
    /// </summary>
    internal sealed class HotkeyWindow : NativeWindow, IDisposable
    {
        private const int HotkeyId = 1;
        private bool registered;

        public event EventHandler Pressed;

        public HotkeyWindow()
        {
            CreateHandle(new CreateParams());
        }

        /// <summary>Registers the shortcut; returns false if another program already uses it.</summary>
        public bool Register(Hotkey hotkey)
        {
            Unregister();
            registered = NativeMethods.RegisterHotKey(Handle, HotkeyId,
                hotkey.Modifiers | NativeMethods.MOD_NOREPEAT, (uint)hotkey.Key);
            return registered;
        }

        public void Unregister()
        {
            if (registered)
            {
                NativeMethods.UnregisterHotKey(Handle, HotkeyId);
                registered = false;
            }
        }

        protected override void WndProc(ref Message m)
        {
            if (m.Msg == NativeMethods.WM_HOTKEY && m.WParam.ToInt32() == HotkeyId)
            {
                Pressed?.Invoke(this, EventArgs.Empty);
            }
            base.WndProc(ref m);
        }

        public void Dispose()
        {
            Unregister();
            DestroyHandle();
        }
    }
}
