namespace HelloWorld
{
    class Hello {
        static void Main(string[] args)
        {
            for (int i = 0; i < 4; i++) {
                System.Console.WriteLine("line " + i);
                System.Threading.Thread.Sleep(500);
            }
        }
    }
}