using System;
using System.Drawing;
using System.Drawing.Text;
using System.Windows.Forms;

namespace UnicodeToNudi
{
    /// <summary>Settings window: shortcut, Nudi font, start with Windows, and a quick test box.</summary>
    internal sealed class SettingsForm : Form
    {
        private readonly ComboBox hotkeyBox = new ComboBox();
        private readonly ComboBox fontBox = new ComboBox();
        private readonly CheckBox startupBox = new CheckBox();
        private readonly TextBox testInput = new TextBox();
        private readonly TextBox testOutput = new TextBox();

        public SettingsForm(Settings settings)
        {
            Text = "Unicode to Nudi " + Program.VersionText + " - Settings";
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            MinimizeBox = false;
            StartPosition = FormStartPosition.CenterScreen;
            AutoScaleMode = AutoScaleMode.Font;
            Font = SystemFonts.MessageBoxFont;
            ClientSize = new Size(460, 400);

            var layout = new TableLayoutPanel
            {
                Dock = DockStyle.Fill,
                Padding = new Padding(12),
                ColumnCount = 2,
                AutoSize = true,
            };
            layout.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
            layout.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));

            var help = new Label
            {
                Text = "Select Kannada Unicode text in Word and press the shortcut. " +
                       "Kannada becomes Nudi in the Nudi font; English words are left unchanged.",
                AutoSize = true,
                MaximumSize = new Size(430, 0),
                Margin = new Padding(0, 0, 0, 10),
            };
            layout.Controls.Add(help, 0, 0);
            layout.SetColumnSpan(help, 2);

            hotkeyBox.DropDownStyle = ComboBoxStyle.DropDownList;
            hotkeyBox.Items.AddRange(Hotkey.Choices);
            hotkeyBox.SelectedItem = settings.Hotkey;
            hotkeyBox.Dock = DockStyle.Fill;
            AddRow(layout, "Shortcut:", hotkeyBox);

            // Editable list: installed Nudi fonts first, then the rest
            fontBox.DropDownStyle = ComboBoxStyle.DropDown;
            fontBox.Dock = DockStyle.Fill;
            using (var fonts = new InstalledFontCollection())
            {
                foreach (FontFamily family in fonts.Families)
                {
                    if (family.Name.IndexOf("Nudi", StringComparison.OrdinalIgnoreCase) >= 0)
                    {
                        fontBox.Items.Add(family.Name);
                    }
                }
                foreach (FontFamily family in fonts.Families)
                {
                    if (family.Name.IndexOf("Nudi", StringComparison.OrdinalIgnoreCase) < 0)
                    {
                        fontBox.Items.Add(family.Name);
                    }
                }
            }
            fontBox.Text = settings.NudiFont;
            fontBox.TextChanged += (s, e) => UpdatePreview();
            AddRow(layout, "Nudi font:", fontBox);

            startupBox.Text = "Start automatically when I sign in to Windows";
            startupBox.AutoSize = true;
            startupBox.Checked = Settings.StartWithWindows;
            layout.Controls.Add(startupBox, 1, layout.RowCount++);

            var testLabel = new Label
            {
                Text = "Try it - type or paste Kannada:",
                AutoSize = true,
                Margin = new Padding(0, 12, 0, 4),
            };
            layout.Controls.Add(testLabel, 0, layout.RowCount);
            layout.SetColumnSpan(testLabel, 2);
            layout.RowCount++;

            testInput.Multiline = true;
            testInput.Height = 50;
            testInput.Dock = DockStyle.Fill;
            testInput.Text = "ಕನ್ನಡ Kannada ಕರ್ನಾಟಕ";
            testInput.TextChanged += (s, e) => UpdatePreview();
            layout.Controls.Add(testInput, 0, layout.RowCount);
            layout.SetColumnSpan(testInput, 2);
            layout.RowCount++;

            testOutput.Multiline = true;
            testOutput.ReadOnly = true;
            testOutput.Height = 50;
            testOutput.Dock = DockStyle.Fill;
            layout.Controls.Add(testOutput, 0, layout.RowCount);
            layout.SetColumnSpan(testOutput, 2);
            layout.RowCount++;

            var buttons = new FlowLayoutPanel
            {
                FlowDirection = FlowDirection.RightToLeft,
                Dock = DockStyle.Bottom,
                AutoSize = true,
                Padding = new Padding(8),
            };
            var cancel = new Button { Text = "Cancel", DialogResult = DialogResult.Cancel, AutoSize = true };
            var ok = new Button { Text = "Save", DialogResult = DialogResult.OK, AutoSize = true };
            buttons.Controls.Add(cancel);
            buttons.Controls.Add(ok);
            AcceptButton = ok;
            CancelButton = cancel;

            Controls.Add(layout);
            Controls.Add(buttons);
            UpdatePreview();
        }

        public Hotkey SelectedHotkey => (Hotkey)hotkeyBox.SelectedItem;

        public string SelectedFont => fontBox.Text.Trim();

        public bool StartWithWindows => startupBox.Checked;

        private static void AddRow(TableLayoutPanel layout, string label, Control control)
        {
            int row = layout.RowCount++;
            layout.Controls.Add(new Label { Text = label, AutoSize = true, Anchor = AnchorStyles.Left }, 0, row);
            layout.Controls.Add(control, 1, row);
        }

        /// <summary>Shows the Nudi text; Kannada-only text is previewed in the Nudi font.</summary>
        private void UpdatePreview()
        {
            testOutput.Text = NudiConverter.Convert(testInput.Text);
            try
            {
                var font = new Font(SelectedFont.Length > 0 ? SelectedFont : "Nudi 01 e", 12f);
                Font old = testOutput.Font;
                testOutput.Font = font;
                if (old != Font)
                {
                    old.Dispose();
                }
            }
            catch (ArgumentException)
            {
                // Font not installed: keep the current font
            }
        }
    }
}
