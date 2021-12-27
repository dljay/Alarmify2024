package com.theglendales.alarm.jjdata

data class RtInTheCloud(val title: String="", val tags: String="", val description: String="", val imageURL: String="", val mp3URL: String="",
                        val id: Int = 0, val iapName: String="", val bdgStrArray: ArrayList<String> = arrayListOf())
{
    override fun toString(): String
    {
        return "ringtoneClass: id =$id, title='$title', iapName= $iapName, bdgStrArray=$bdgStrArray, tags ='$tags', description='$description', \nimage='$imageURL', mp3URL = $mp3URL)"
    }
//DATA CLASS 이기 때문에 아래 fun equals.. fun hashCode() 는 필요 없다고 하는데. 일단은 넣어놓음.

    override fun equals(other: Any?): Boolean { //Here we determine "what it means to have their contents the 'same' "
        //We'll return false if any of the below conditions don't match

        // 1) if javaClass(the actual class of the object) is the same with the compared object
        if(javaClass != other?.javaClass)
        {
            return false
        }
        // 위에서 통과하면 (즉 같은 javaClass의 object 면)
        other as RtInTheCloud

        // 2) Check Individual Fields
        if(id != other.id) return false
        if(imageURL != other.imageURL) return false
        if(mp3URL != other.mp3URL) return false
        if(title != other.title) return false
        if(description != other.description) return false
        if(tags != other.tags) return false
        if(bdgStrArray != other.bdgStrArray) return false
        //todo: iapName 등 없는 부분 추가?


        // 4) 모두 통과하면 return true!
        //Log.d(TAG, "equals: !!! COMPARED TWO ARE IDENTICAL!!!!")
        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + imageURL.hashCode()
        result = 31 * result + mp3URL.hashCode()
        result = 31 * result + id

        return result
    }

}