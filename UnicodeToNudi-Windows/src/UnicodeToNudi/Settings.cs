using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Windows.Forms;
using Microsoft.Win32;

namespace UnicodeToNudi
{
    /// <summary>A keyboard shortcut the user can pick in Settings.</summary>
    internal sealed class Hotkey
    {
        public readonly string Name;
        public readonly uint Modifiers;
        public readonly Keys Key;

        private Hotkey(string name, uint modifiers, Keys key)
        {
            Name = name;
            Modifiers = modifiers;
            Key = key;
        }

        public override string ToString()
        {
            return Name;
        }

        public static readonly Hotkey[] Choices =
        {
            new Hotkey("Ctrl+Alt+N", NativeMethods.MOD_CONTROL | NativeMethods.MOD_ALT, Keys.N),
            new Hotkey("Ctrl+Alt+K", NativeMethods.MOD_CONTROL | NativeMethods.MOD_ALT, Keys.K),
            new Hotkey("Ctrl+Shift+Alt+N", NativeMethods.MOD_CONTROL | NativeMethods.MOD_SHIFT | NativeMethods.MOD_ALT, Keys.N),
            new Hotkey("Ctrl+Alt+F12", NativeMethods.MOD_CONTROL | NativeMethods.MOD_ALT, Keys.F12),
            new Hotkey("Win+Alt+N", NativeMethods.MOD_WIN | NativeMethods.MOD_ALT, Keys.N),
        };

        public static Hotkey Find(string name)
        {
            foreach (Hotkey h in Choices)
            {
                if (string.Equals(h.Name, name, StringComparison.OrdinalIgnoreCase))
                {
                    return h;
                }
            }
            return Choices[0];
        }
    }

    /// <summary>User settings, saved in %APPDATA%\UnicodeToNudi\settings.ini.</summary>
    internal sealed class Settings
    {
        private const string RunKey = @"Software\Microsoft\Windows\CurrentVersion\Run";
        private const string RunValue = "UnicodeToNudi";

        public Hotkey Hotkey = Hotkey.Choices[0];
        public string NudiFont = "Nudi 01 e";

        /// <summary>True when no settings file existed yet, i.e. the app's first run.</summary>
        public bool IsFirstRun;

        private static string FilePath
        {
            get
            {
                string dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "UnicodeToNudi");
                return Path.Combine(dir, "settings.ini");
            }
        }

        public static Settings Load()
        {
            var settings = new Settings { IsFirstRun = !File.Exists(FilePath) };
            try
            {
                if (!settings.IsFirstRun)
                {
                    foreach (string line in File.ReadAllLines(FilePath, Encoding.UTF8))
                    {
                        int eq = line.IndexOf('=');
                        if (eq <= 0)
                        {
                            continue;
                        }
                        string key = line.Substring(0, eq).Trim();
                        string value = line.Substring(eq + 1).Trim();
                        if (key == "Hotkey")
                        {
                            settings.Hotkey = Hotkey.Find(value);
                        }
                        else if (key == "NudiFont" && value.Length > 0)
                        {
                            settings.NudiFont = value;
                        }
                    }
                }
            }
            catch (Exception)
            {
                // Broken settings file: use defaults
            }
            return settings;
        }

        public void Save()
        {
            Directory.CreateDirectory(Path.GetDirectoryName(FilePath));
            var lines = new List<string>
            {
                "Hotkey=" + Hotkey.Name,
                "NudiFont=" + NudiFont,
            };
            File.WriteAllLines(FilePath, lines, Encoding.UTF8);
        }

        /// <summary>Whether the app starts automatically when the user signs in to Windows.</summary>
        public static bool StartWithWindows
        {
            get
            {
                using (RegistryKey key = Registry.CurrentUser.OpenSubKey(RunKey))
                {
                    return key != null && key.GetValue(RunValue) != null;
                }
            }
            set
            {
                using (RegistryKey key = Registry.CurrentUser.CreateSubKey(RunKey))
                {
                    if (value)
                    {
                        key.SetValue(RunValue, "\"" + Application.ExecutablePath + "\" " + Program.BackgroundArg);
                    }
                    else
                    {
                        key.DeleteValue(RunValue, false);
                    }
                }
            }
        }
    }
}
