namespace java com.github.dapeng.soa.service

service CalculateService{

    i32 calcualteWordCount(1:string filename,2:string word),

    map<string, i32> calcualteWordsCount(1:string fileName)


}