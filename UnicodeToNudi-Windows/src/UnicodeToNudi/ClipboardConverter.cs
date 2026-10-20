using System;
using System.Threading;
using System.Windows.Forms;

namespace UnicodeToNudi
{
    /// <summary>
    /// Fallback for programs other than Word (Notepad, browsers, ...):
    /// copy the selection, convert it, and paste the Nudi text back.
    /// English text is kept as it is; fonts can't be changed this way.
    /// </summary>
    internal static class ClipboardConverter
    {
        public enum Outcome
        {
            Converted,
            NothingSelected,
            NoKannada,
        }

        public static Outcome ConvertSelection()
        {
            // The user may still be holding Ctrl/Alt/Shift from the shortcut;
            // our Ctrl+C would then become e.g. Ctrl+Alt+C.
            WaitForModifierRelease(TimeSpan.FromSeconds(2));

            string saved = Clipboard.ContainsText() ? Clipboard.GetText() : null;
            try
            {
                Clipboard.Clear();
                NativeMethods.SendCtrl((byte)'C');

                string selected = WaitForClipboardText(TimeSpan.FromMilliseconds(800));
                if (string.IsNullOrEmpty(selected))
                {
                    return Outcome.NothingSelected;
                }
                if (!NudiConverter.ContainsKannada(selected))
                {
                    return Outcome.NoKannada;
                }

                Clipboard.SetText(NudiConverter.Convert(selected));
                NativeMethods.SendCtrl((byte)'V');
                // Give the target program time to read the clipboard before restoring it
                Thread.Sleep(400);
                return Outcome.Converted;
            }
            finally
            {
                try
                {
                    if (saved != null)
                    {
                        Clipboard.SetText(saved);
                    }
                    else
                    {
                        Clipboard.Clear();
                    }
                }
                catch (Exception)
                {
                    // Clipboard busy; leave it
                }
            }
        }

        private static void WaitForModifierRelease(TimeSpan timeout)
        {
            DateTime until = DateTime.UtcNow + timeout;
            while (DateTime.UtcNow < until &&
                   (NativeMethods.IsKeyDown(NativeMethods.VK_CONTROL) ||
                    NativeMethods.IsKeyDown(NativeMethods.VK_MENU) ||
                    NativeMethods.IsKeyDown(NativeMethods.VK_SHIFT) ||
                    NativeMethods.IsKeyDown(NativeMethods.VK_LWIN) ||
                    NativeMethods.IsKeyDown(NativeMethods.VK_RWIN)))
            {
                Thread.Sleep(20);
            }
        }

        private static string WaitForClipboardText(TimeSpan timeout)
        {
            DateTime until = DateTime.UtcNow + timeout;
            while (DateTime.UtcNow < until)
            {
                Application.DoEvents();
                try
                {
                    if (Clipboard.ContainsText())
                    {
                        return Clipboard.GetText();
                    }
                }
                catch (Exception)
                {
                    // Clipboard locked by the other program; retry
                }
                Thread.Sleep(30);
            }
            return null;
        }
    }
}
