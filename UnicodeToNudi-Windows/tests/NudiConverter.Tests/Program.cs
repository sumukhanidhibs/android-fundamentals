using System;
using System.IO;
using System.Text;
using UnicodeToNudi;

// Checks the C# NudiConverter against vectors.tsv, which is generated from the
// Java (Android) converter, so both apps produce identical Nudi text.
// Run with: dotnet run --project tests/NudiConverter.Tests
internal static class Program
{
    private static int Main()
    {
        string path = Path.Combine(AppContext.BaseDirectory, "vectors.tsv");
        int passed = 0;
        int failed = 0;
        foreach (string line in File.ReadAllLines(path, Encoding.UTF8))
        {
            if (line.Length == 0 || line.StartsWith("#"))
            {
                continue;
            }
            string[] parts = line.Split('\t');
            string actual = NudiConverter.Convert(parts[0]);
            if (actual == parts[1])
            {
                passed++;
            }
            else
            {
                failed++;
                Console.WriteLine($"FAIL {parts[0]}\n  expected: {parts[1]}\n  actual:   {actual}");
            }
        }

        Check(ref passed, ref failed, NudiConverter.ContainsKannada("hello ಕನ್ನಡ"), "ContainsKannada(mixed)");
        Check(ref passed, ref failed, !NudiConverter.ContainsKannada("hello"), "ContainsKannada(English)");

        Console.WriteLine($"{passed} passed, {failed} failed");
        return failed == 0 && passed > 0 ? 0 : 1;
    }

    private static void Check(ref int passed, ref int failed, bool ok, string name)
    {
        if (ok)
        {
            passed++;
        }
        else
        {
            failed++;
            Console.WriteLine("FAIL " + name);
        }
    }
}
