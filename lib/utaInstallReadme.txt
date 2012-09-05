
    * Copy the lp_solve dynamic libraries from the archives lp_solve_5.5_dev.(zip or tar.gz)  and lp_solve_5.5_exe.(zip or tar.gz) to a standard library directory for your target platform. On Windows, a typical place would be \WINDOWS or \WINDOWS\SYSTEM32. On Linux, a typical place would be the directory /usr/local/lib.
    * Unzip the Java wrapper distribution file to new directory of your choice.
    * On Windows, copy the wrapper stub library lpsolve55j.dll to the directory that already contains lpsolve55.dll.
    * On Linux, copy the wrapper stub library liblpsolve55j.so to the directory that already contains liblpsolve55.so. Run ldconfig to include the library in the shared libray cache.

	Download files at 
		http://sourceforge.net/projects/lpsolve/files/
		


	