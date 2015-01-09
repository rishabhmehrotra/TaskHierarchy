package core;


public class StatUtility {
	
    // GAMMA FUNCTIONS
    //  Lanczos Gamma Function approximation - N (number of coefficients -1)
    private static int lgfN = 6;
    //  Lanczos Gamma Function approximation - Coefficients
    private static double[] lgfCoeff = {1.000000000190015, 76.18009172947146, -86.50532032941677, 24.01409824083091, -1.231739572450155, 0.1208650973866179E-2, -0.5395239384953E-5};
    //  Lanczos Gamma Function approximation - small gamma
    private static double lgfGamma = 5.0;
	
	
 // Beta function
    // retained for compatibility reasons
    public static double beta(double z, double w){
            return Math.exp(logGamma(z) + logGamma(w) - logGamma(z + w));
    }
    
    
    public static int factorial(int num)
    {
    	int result = 1;
    	for(int i=2;i<=num;i++)
    	{
    		result*=i;
    	}
    	return result;
    }
    
    
    
    
	// Gamma function
    // Lanczos approximation (6 terms)
    // retained for backward compatibity
    public static double gamma(double x){

    	
            double xcopy = x;
            double first = x + lgfGamma + 0.5;
            double second = lgfCoeff[0];
            double fg = 0.0D;

            if(x>=0.0){
                    first = Math.pow(first, x + 0.5)*Math.exp(-first);
                    for(int i=1; i<=lgfN; i++)second += lgfCoeff[i]/++xcopy;
                    fg = first*Math.sqrt(2.0*Math.PI)*second/x;
            }
            else{
                     fg = -Math.PI/(x*StatUtility.gamma(-x)*Math.sin(Math.PI*x));
            }
            //System.out.println("called for gamma(_"+x+"_) & returning: "+fg);
            return fg;
    }
    
    
    
 // log to base e of the Gamma function
    // Lanczos approximation (6 terms)
    // Retained for backward compatibility
    public static double logGamma(double x){
            double xcopy = x;
            double fg = 0.0D;
            double first = x + lgfGamma + 0.5;
            double second = lgfCoeff[0];

            if(x>=0.0){
                    first -= (x + 0.5)*Math.log(first);
                    for(int i=1; i<=lgfN; i++)second += lgfCoeff[i]/++xcopy;
                    fg = Math.log(Math.sqrt(2.0*Math.PI)*second/x) - first;
            }
            else{
                    fg = Math.PI/(gamma(1.0D-x)*Math.sin(Math.PI*x));

                    if(fg!=1.0/0.0 && fg!=-1.0/0.0){
                            if(fg<0){
                                     throw new IllegalArgumentException("\nThe gamma function is negative");
                            }
                            else{
                                    fg = Math.log(fg);
                            }
                    }
            }
            return fg;
    }

}
