using System;
using System.Drawing;
using System.Drawing.Text;
using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace UnicodeToNudi
{
    /// <summary>
    /// Runs in the background as an icon in the notification area (system tray)
    /// and converts the current selection when the shortcut is pressed.
    /// </summary>
    internal sealed class TrayApp : ApplicationContext
    {
        private const string WordWindowClass = "OpusApp";

        private readonly Settings settings;
        private readonly NotifyIcon trayIcon;
        private readonly HotkeyWindow hotkeyWindow;
        private readonly ToolStripMenuItem convertItem;
        private bool busy;

        public TrayApp(bool quietStart)
        {
            settings = Settings.Load();
            if (settings.IsFirstRun)
            {
                // Start with Windows by default; can be switched off in Settings
                try
                {
                    Settings.StartWithWindows = true;
                    settings.Save();
                }
                catch (Exception)
                {
                    // Not fatal: the user can still enable it in Settings
                }
            }

            convertItem = new ToolStripMenuItem("Convert selection", null, (s, e) => ConvertSelection());
            var menu = new ContextMenuStrip();
            menu.Items.Add(convertItem);
            menu.Items.Add("Settings...", null, (s, e) => ShowSettings());
            menu.Items.Add(new ToolStripSeparator());
            menu.Items.Add("Exit", null, (s, e) => ExitThread());

            trayIcon = new NotifyIcon
            {
                Icon = CreateIcon(),
                ContextMenuStrip = menu,
                Visible = true,
            };
            trayIcon.DoubleClick += (s, e) => ShowSettings();

            hotkeyWindow = new HotkeyWindow();
            hotkeyWindow.Pressed += (s, e) => ConvertSelection();
            RegisterHotkey();

            if (!IsFontInstalled(settings.NudiFont))
            {
                Notify("Font not found",
                    "\"" + settings.NudiFont + "\" is not installed. Install the Nudi fonts or pick the right font in Settings.",
                    ToolTipIcon.Warning);
            }
            else if (!quietStart)
            {
                Notify("Unicode to Nudi is running",
                    "Select Kannada text in Word and press " + settings.Hotkey.Name + ".",
                    ToolTipIcon.Info);
            }
        }

        private void RegisterHotkey()
        {
            bool ok = hotkeyWindow.Register(settings.Hotkey);
            trayIcon.Text = "Unicode to Nudi (" + settings.Hotkey.Name + ")";
            convertItem.Text = "Convert selection (" + settings.Hotkey.Name + ")";
            if (!ok)
            {
                Notify("Shortcut unavailable",
                    settings.Hotkey.Name + " is used by another program. Choose a different shortcut in Settings.",
                    ToolTipIcon.Warning);
            }
        }

        private void ConvertSelection()
        {
            if (busy)
            {
                return;
            }
            busy = true;
            try
            {
                if (NativeMethods.GetForegroundClassName() == WordWindowClass && ConvertInWord())
                {
                    return;
                }
                ConvertWithClipboard();
            }
            finally
            {
                busy = false;
            }
        }

        /// <summary>Returns false if Word could not be reached, so the clipboard method is used instead.</summary>
        private bool ConvertInWord()
        {
            object word = WordConverter.GetRunningWord();
            if (word == null)
            {
                return false;
            }
            try
            {
                WordConverter.Result result = WordConverter.ConvertSelection(word, settings.NudiFont);
                if (!result.HadSelection)
                {
                    Notify("Nothing selected", "Select the Kannada text first, then press " + settings.Hotkey.Name + ".", ToolTipIcon.Info);
                }
                else if (result.Converted == 0 && result.Skipped == 0)
                {
                    Notify("No Kannada text", "The selection has no Kannada Unicode text.", ToolTipIcon.Info);
                }
                else if (result.Skipped > 0)
                {
                    Notify("Partly converted",
                        result.Skipped + " Kannada word(s) next to fields or special objects were not converted. Select them separately and try again.",
                        ToolTipIcon.Warning);
                }
                return true;
            }
            catch (COMException ex)
            {
                Notify("Word is busy",
                    "Close any open dialog box in Word and try again. (" + ex.Message + ")",
                    ToolTipIcon.Warning);
                return true;
            }
            finally
            {
                Marshal.ReleaseComObject(word);
            }
        }

        private void ConvertWithClipboard()
        {
            try
            {
                switch (ClipboardConverter.ConvertSelection())
                {
                    case ClipboardConverter.Outcome.NothingSelected:
                        Notify("Nothing selected", "Select the Kannada text first, then press " + settings.Hotkey.Name + ".", ToolTipIcon.Info);
                        break;
                    case ClipboardConverter.Outcome.NoKannada:
                        Notify("No Kannada text", "The selection has no Kannada Unicode text.", ToolTipIcon.Info);
                        break;
                }
            }
            catch (Exception ex)
            {
                Notify("Could not convert", ex.Message, ToolTipIcon.Error);
            }
        }

        private void ShowSettings()
        {
            using (var form = new SettingsForm(settings))
            {
                if (form.ShowDialog() != DialogResult.OK)
                {
                    return;
                }
                settings.Hotkey = form.SelectedHotkey;
                if (form.SelectedFont.Length > 0)
                {
                    settings.NudiFont = form.SelectedFont;
                }
                try
                {
                    settings.Save();
                    Settings.StartWithWindows = form.StartWithWindows;
                }
                catch (Exception ex)
                {
                    Notify("Could not save settings", ex.Message, ToolTipIcon.Error);
                }
                RegisterHotkey();
            }
        }

        private void Notify(string title, string text, ToolTipIcon icon)
        {
            trayIcon.ShowBalloonTip(4000, title, text, icon);
        }

        private static bool IsFontInstalled(string name)
        {
            using (var fonts = new InstalledFontCollection())
            {
                foreach (FontFamily family in fonts.Families)
                {
                    if (string.Equals(family.Name, name, StringComparison.OrdinalIgnoreCase))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        /// <summary>Draws the tray icon: a white "ಕ" on a blue square.</summary>
        private static Icon CreateIcon()
        {
            using (var bitmap = new Bitmap(32, 32))
            using (Graphics g = Graphics.FromImage(bitmap))
            using (var background = new SolidBrush(Color.FromArgb(0x1E, 0x5A, 0xA8)))
            using (var font = new Font("Nirmala UI", 15f, FontStyle.Bold, GraphicsUnit.Pixel))
            {
                g.SmoothingMode = System.Drawing.Drawing2D.SmoothingMode.AntiAlias;
                g.TextRenderingHint = TextRenderingHint.AntiAliasGridFit;
                g.FillRectangle(background, 0, 0, 32, 32);
                TextRenderer.DrawText(g, "ಕ", font, new Rectangle(0, 0, 32, 32), Color.White,
                    TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
                return Icon.FromHandle(bitmap.GetHicon());
            }
        }

        protected override void ExitThreadCore()
        {
            hotkeyWindow.Dispose();
            trayIcon.Visible = false;
            trayIcon.Dispose();
            base.ExitThreadCore();
        }
    }
}
